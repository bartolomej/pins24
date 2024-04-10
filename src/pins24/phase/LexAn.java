package pins24.phase;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pins24.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@code nextChar}). */
	private int buffChar = '\n';

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@code nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@code nextChar}). */
	private int buffCharColumn = 0;

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
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 8 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
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
		buffToken = null;
		while (buffToken == null) {
			tryReadNumberToken();

			tryReadKeywordOrIdentifierToken();

			switch (buffChar) {
				case '\n':
				case ' ':
				case '\t':
				case '\r':
					nextChar();
					break;
				case '\'':
					readCharToken();
					break;
				case ',':
					makeToken(Token.Symbol.COMMA);
					nextChar();
					break;
				case '(':
					makeToken(Token.Symbol.LPAREN);
					nextChar();
					break;
				case ')':
					makeToken(Token.Symbol.RPAREN);
					nextChar();
					break;
				case '^':
					makeToken(Token.Symbol.PTR);
					nextChar();
					break;
				case '%':
					makeToken(Token.Symbol.MOD);
					nextChar();
					break;
				case '/':
					makeToken(Token.Symbol.DIV);
					nextChar();
					break;
				case '*':
					makeToken(Token.Symbol.MUL);
					nextChar();
					break;
				case '-':
					makeToken(Token.Symbol.SUB);
					nextChar();
					break;
				case '+':
					makeToken(Token.Symbol.ADD);
					nextChar();
					break;
				case '&':
					nextChar();
					matchOrThrow("&");
					makeToken(Token.Symbol.AND);
					nextChar();
					break;
				case '|':
					nextChar();
					matchOrThrow("|");
					makeToken(Token.Symbol.OR);
					nextChar();
					break;
				case '=':
					nextChar();
					if (buffChar == '=') {
						makeToken(Token.Symbol.EQU);
						nextChar();
					} else {
						makeToken(Token.Symbol.ASSIGN);
					}
					break;
				case '!':
					nextChar();
					if (buffChar == '=') {
						makeToken(Token.Symbol.NEQ);
						nextChar();
					} else {
						makeToken(Token.Symbol.NOT);
					}
					break;
				case '>':
					nextChar();
					if (buffChar == '=') {
						makeToken(Token.Symbol.GEQ);
						nextChar();
					} else {
						makeToken(Token.Symbol.GTH);
					}
					break;
				case '<':
					nextChar();
					if (buffChar == '=') {
						makeToken(Token.Symbol.LEQ);
						nextChar();
					} else {
						makeToken(Token.Symbol.LTH);
					}
					break;
				case -1:
					break;
				default:
					throw unexpectedTokenError();
			}
		}
	}

	private void readCharToken() {
		nextChar();
		StringBuilder lexeme = new StringBuilder();
		if (buffChar == '\\') {
			nextChar();
			if (buffChar == 'n') {
				lexeme.append("\n");
			} else if (buffChar == '\'') {
				lexeme.append("'");
			} else if (buffChar == '\\') {
				lexeme.append("\\");
			} else {
				String asciiCode = "";
				if (matchesPattern("([0-9]|[A-F])", buffChar)) {
					asciiCode += (char)buffChar;
				} else {
					throw unexpectedTokenError();
				}
				if (matchesPattern("([0-9]|[A-F])", buffChar)) {
					asciiCode += (char)buffChar;
				} else {
					throw unexpectedTokenError();
				}
				nextChar();
				lexeme.append(Character.toString(
						Integer.parseInt(asciiCode,16)
				));

			}
		} else if (matchesPattern("[\\x20-\\x7E]", buffChar)) {
			lexeme.append((char)buffChar);
		} else {
			throw unexpectedTokenError();
		}
		nextChar();
		if ((buffChar) != '\'') {
			throw unexpectedTokenError();
		}
		this.buffToken = new Token(getCurrentLocation(), Token.Symbol.CHARCONST, lexeme.toString());
		nextChar();
	}

	private void tryReadNumberToken() {
		if (!matchesPattern("[0-9]", buffChar)) {
			return;
		}
		StringBuilder lexeme = new StringBuilder();
		while (matchesPattern("[0-9]", buffChar)) {
            lexeme.append((char) buffChar);
            nextChar();
        }
		this.buffToken = new Token(getCurrentLocation(), Token.Symbol.INTCONST, lexeme.toString());
	}

	private void tryReadKeywordOrIdentifierToken() {
		if (!matchesPattern("[_a-z]", buffChar)) {
			return;
		}
		StringBuilder lexeme = new StringBuilder();
		while (matchesPattern("[_0-9a-z]", buffChar)) {
			lexeme.append((char) buffChar);
			nextChar();
		}
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
		this.buffToken = new Token(getCurrentLocation(), symbol, lexeme.toString());
	}

	private Report.Error unexpectedTokenError() {
		return new Report.Error(getCurrentLocation(), "Unexpected token: " + (char)(buffChar));
	}

	private Report.Location getCurrentLocation() {
		return new Report.Location(this.buffCharLine, this.buffCharColumn);
	}

	private void makeToken(Token.Symbol symbol) {
		this.buffToken = new Token(getCurrentLocation(), symbol, (char)buffChar + "");
	}

	private void matchOrThrow(String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher((char)buffChar + "");
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

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken() != null)
					System.out.println(lexAn.takeToken());
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
		}
	}

}
