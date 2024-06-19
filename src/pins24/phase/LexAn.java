package pins24.phase;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pins24.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;
	/** Character position within `consumedChars` list. **/
	private int startOffset;
	private int endOffset;
	private final List<Integer> consumedChars;
	/**
	 * Start/end positions of the currently consuming token.
	 */
	private Report.Location startPosition;

	/** End Of File - no characters left to read  **/
	private final int EOF = -1;
	/** Start Of File - no character was read  **/
	private final int SOF = -2;

	/**
	 * Ustvari nov leksikalni analizator.
	 */
	public LexAn(final Reader reader) {
		this.srcFile = new BufferedReader(reader);
		this.consumedChars = new ArrayList<>();
		this.consumedChars.add(SOF);
		this.startPosition = new Report.Location(1, 1);
		this.endOffset = 0;
		this.startOffset = 0;
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 *
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@code buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@code buffCharLine} in
	 * {@code buffCharColumn}.
	 *
	 * Zacetne vrednosti {@code buffChar}, {@code buffCharLine} in
	 * {@code buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@code buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 *
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@code buffChar} ves "cas veljaven znak. Zunaj metode {@code nextChar} so vse
	 * spremenljivke {@code buffChar}, {@code buffCharLine} in
	 * {@code buffCharColumn} namenjene le branju.
	 *
	 * Vrednost {@code -1} v spremenljivki {@code buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@code buffCharLine} in {@code buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		if (currentChar() == EOF) {
			return;
		}
		if (endOffset < consumedChars.size() - 1) {
			endOffset++;
			return;
		}
		try {
			this.consumedChars.add(srcFile.read());
			this.endOffset++;
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	private int currentChar() {
		return this.consumedChars.get(this.endOffset);
	}

	private void stepBack() {
		this.endOffset--;
	}

	/**
	 * Trenutni leksikalni simbol.
	 *
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@code peekToken} in {@code takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@code peekToken} in {@code takeToken}.
	 */
	private void nextToken() {
		nextChar();

		buffToken = null;
		while (buffToken == null) {
			switch (currentChar()) {
				case '#':
					skipLineComment();
					break;
				case '\n':
				case ' ':
				case '\t':
				case '\r':
					skipToken();
					break;
				case '"':
					nextChar();
					StringBuilder stringLexeme = new StringBuilder();
					while (currentChar() != '"') {
						stringLexeme.append(readCharLexeme('"'));
						nextChar();
					}
					this.makeToken(Token.Symbol.STRINGCONST, stringLexeme.toString());
					break;
				case '\'':
					String charLexeme = readCharLexeme('\'');
					nextChar();
					if (currentChar() != '\'') {
						throw unexpectedTokenError();
					}
					this.makeToken(Token.Symbol.CHARCONST, charLexeme);
					break;
				case ',':
					makeToken(Token.Symbol.COMMA);
					break;
				case '(':
					makeToken(Token.Symbol.LPAREN);
					break;
				case ')':
					makeToken(Token.Symbol.RPAREN);
					break;
				case '^':
					makeToken(Token.Symbol.PTR);
					break;
				case '%':
					makeToken(Token.Symbol.MOD);
					break;
				case '/':
					makeToken(Token.Symbol.DIV);
					break;
				case '*':
					makeToken(Token.Symbol.MUL);
					break;
				case '-':
					makeToken(Token.Symbol.SUB);
					break;
				case '+':
					makeToken(Token.Symbol.ADD);
					break;
				case '&':
					nextChar();
					matchOrThrow("&");
					makeToken(Token.Symbol.AND);
					break;
				case '|':
					nextChar();
					matchOrThrow("|");
					makeToken(Token.Symbol.OR);
					break;
				case '=':
					nextChar();
					if (currentChar() == '=') {
						makeToken(Token.Symbol.EQU);
					} else {
						stepBack();
						makeToken(Token.Symbol.ASSIGN);
					}
					break;
				case '!':
					nextChar();
					if (currentChar() == '=') {
						makeToken(Token.Symbol.NEQ);
					} else {
						stepBack();
						makeToken(Token.Symbol.NOT);
					}
					break;
				case '>':
					nextChar();
					if (currentChar() == '=') {
						makeToken(Token.Symbol.GEQ);
					} else {
						stepBack();
						makeToken(Token.Symbol.GTH);
					}
					break;
				case '<':
					nextChar();
					if (currentChar() == '=') {
						makeToken(Token.Symbol.LEQ);
					} else {
						stepBack();
						makeToken(Token.Symbol.LTH);
					}
					break;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					consumeNumber();
					break;
				case EOF:
					makeToken(Token.Symbol.EOF);
					break;
				default: {
					if (matchesPattern("[_a-z]", currentChar())) {
						consumeKeywordOrIdentifier();
					} else {
						throw unexpectedTokenError();
					}
				}
			}
		}
	}

	private void skipLineComment() {
		while (currentChar() != '\n' && currentChar() != EOF) {
			skipToken();
		}
	}

	private String readCharLexeme(char caretLexeme) {
		StringBuilder lexeme = new StringBuilder();
		if (currentChar() == '\\') {
			nextChar();
			if (currentChar() == 'n') {
				lexeme.append("\n");
			} else if (currentChar() == caretLexeme) {
				lexeme.append("" + caretLexeme);
			} else if (currentChar() == '\\') {
				lexeme.append("\\");
			} else {
				String asciiCode = "";
				if (matchesPattern("([0-9]|[A-F])", currentChar())) {
					asciiCode += (char) currentChar();
				} else {
					throw unexpectedTokenError();
				}
				nextChar();
				if (matchesPattern("([0-9]|[A-F])", currentChar())) {
					asciiCode += (char) currentChar();
				} else {
					throw unexpectedTokenError();
				}
				int decAsciiCode = Integer.parseInt(asciiCode,16);
				lexeme.append(Character.toString(decAsciiCode));
			}
		} else if (matchesPattern("[\\x20-\\x7E]", currentChar())) {
			lexeme.append((char) currentChar());
		} else {
			throw unexpectedTokenError();
		}
		return lexeme.toString();
	}

	private void consumeNumber() {
		StringBuilder lexeme = new StringBuilder();
		while (matchesPattern("[0-9]", currentChar())) {
            lexeme.append((char) currentChar());
            nextChar();
        }
		this.makeToken(Token.Symbol.INTCONST, lexeme.toString());
	}

	private void consumeKeywordOrIdentifier() {
		StringBuilder lexeme = new StringBuilder();
		while (matchesPattern("[_0-9a-z]", currentChar())) {
			lexeme.append((char) currentChar());
			nextChar();
		}
		// We stepped forward one character extra above
		stepBack();

		Token.Symbol symbol = switch (lexeme.toString()) {
			case "fun" -> Token.Symbol.FUN;
			case "var" -> Token.Symbol.VAR;
			case "if" -> Token.Symbol.IF;
			case "then" -> Token.Symbol.THEN;
			case "else" -> Token.Symbol.ELSE;
			case "while" -> Token.Symbol.WHILE;
			case "do" -> Token.Symbol.DO;
			case "let" -> Token.Symbol.LET;
			case "in" -> Token.Symbol.IN;
			case "end" -> Token.Symbol.END;
			default -> Token.Symbol.IDENTIFIER;
		};
        this.makeToken(symbol, lexeme.toString());
	}

	private Report.Error unexpectedTokenError() {
		String charToPrint = switch (currentChar()) {
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            case '\r' -> "\\r";
            default -> (char) currentChar() + "";
        };
        return this.error("Unexpected token: " + charToPrint+ " (" + currentChar() +")");
	}

    private Report.Error error(String message) {
        return new Report.Error(
                getEndPosition(),
                message
        );
    }

	private Report.Location getEndPosition() {
		int line = startPosition.begLine();
		int col = startPosition.begColumn();
		for (int i = startOffset; i <= endOffset; i++) {
			int ch = consumedChars.get(i);
			switch (ch) {
				case '\n' -> {
					line++;
					col = 1;
				}
				case '\t' -> {
					while (col % 8 != 0) {
						col += 1;
					}
					col += 1;
				}
				case SOF, EOF -> {}
				default -> col += 1;
			}
		}
		return new Report.Location(line, col);
	}

	private void makeToken(Token.Symbol symbol) {
		this.makeToken(symbol, (char) currentChar() + "");
	}

	private void skipToken() {
		// Steps forward and prepares the positions for the next token,
		// without creating a token like `makeToken`
		this.startPosition = getEndPosition();
		this.startOffset = this.endOffset + 1;
		nextChar();
	}

	private void makeToken(Token.Symbol symbol, String lexeme) {
		Report.Location endPosition = getEndPosition();
        Report.Location location = new Report.Location(
                startPosition.begLine(),
                startPosition.begColumn(),
                endPosition.begLine(),
                endPosition.begColumn()
        );
        this.buffToken = new Token(location, symbol, lexeme);
		this.startPosition = endPosition;
		this.startOffset = this.endOffset + 1;
	}

	private void matchOrThrow(String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher((char) currentChar() + "");
		if (!matcher.find()) {
			throw unexpectedTokenError();
		}
	}

	private boolean matchesPattern(String regex, int input) {
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher((char)input + "");
		return matcher.find();
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 *
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 *
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 *
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cmdLineArgs[0])));
			try (LexAn lexAn = new LexAn(reader)) {
				while (true) {
					Token token = lexAn.takeToken();
					System.out.println(token);
					if (token.symbol() == Token.Symbol.EOF) {
						break;
					}
				}
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			error.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
