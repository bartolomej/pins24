package pins24.phase;

import pins24.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 * 
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token consume(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol) {
			throw new Report.Error(token, "Unexpected symbol " + token.symbol() + ": '" + token.lexeme() + "'.");
		}
		System.out.println(symbol);
		return token;
	}

	private boolean match(Token.Symbol expectedSymbol) {
		return lexAn.peekToken().symbol() == expectedSymbol;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		parseProgram();
		if (!match(Token.Symbol.EOF))
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private void parseProgram() {
		parseDefinitions();
	}

	private void parseDefinitions() {
		while (true) {
			switch (lexAn.peekToken().symbol()) {
				case FUN -> parseFunDefinition();
				case VAR -> parseVarDefinition();
				default -> {
					return;
				}
			}
		}
	}

	private void parseFunDefinition() {
		consume(Token.Symbol.FUN);
		consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.LPAREN);
		parseParameters();
		consume(Token.Symbol.RPAREN);
		if (match(Token.Symbol.ASSIGN)) {
			parseStatements();
		}
	}

	private void parseParameters() {
		if (!match(Token.Symbol.IDENTIFIER)) {
			return;
		}
		consume(Token.Symbol.IDENTIFIER);
		do {
			if (match(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				consume(Token.Symbol.IDENTIFIER);
			}
		} while (match(Token.Symbol.COMMA));
	}

	private void parseStatements() {
		do {
			parseStatement();
		} while (match(Token.Symbol.COMMA));
	}

	private void parseStatement() {
		switch (lexAn.peekToken().symbol()) {
			case IF -> parseIfStatement();
			case WHILE -> parseWhileStatement();
			case LET -> parseLetStatement();
			default -> parseExpressionOrAssignmentStatement();
		}
	}

	private void parseIfStatement() {
		consume(Token.Symbol.IF);
		parseExpression();
		consume(Token.Symbol.THEN);
		parseStatements();

		if (match(Token.Symbol.ELSE)) {
			consume(Token.Symbol.ELSE);
			parseStatements();
		}

		consume(Token.Symbol.END);
	}

	private void parseWhileStatement() {
		  consume(Token.Symbol.WHILE);
		  parseExpression();
		  consume(Token.Symbol.DO);
		  parseStatements();
		  consume(Token.Symbol.END);
	}

	private void parseLetStatement() {
		consume(Token.Symbol.LET);
		parseDefinitions();
		consume(Token.Symbol.IN);
		parseStatements();
		consume(Token.Symbol.END);
	}

	private void parseExpressionOrAssignmentStatement() {

	}

	private void parseExpression() {

	}

	private void parseVarDefinition() {
		consume(Token.Symbol.VAR);
		consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.ASSIGN);
		parseInitializers();
	}

	private void parseInitializers() {
		do {
			parseInitializer();
		} while (match(Token.Symbol.COMMA));
	}

	private void parseInitializer() {
		if (match(Token.Symbol.INTCONST)) {
			consume(Token.Symbol.INTCONST);
			if (match(Token.Symbol.MUL)) {
				consume(Token.Symbol.MUL);
				parseConst(false);
			}
		} else {
			parseConst(true);
		}
	}

	private void parseConst(boolean isOptional) {
		boolean isMatch = false;
		if (match(Token.Symbol.INTCONST)) {
			consume(Token.Symbol.INTCONST);
			isMatch = true;
		}
		if (match(Token.Symbol.CHARCONST)) {
			consume(Token.Symbol.CHARCONST);
			isMatch = true;
		}
		if (match(Token.Symbol.STRINGCONST)) {
			consume(Token.Symbol.STRINGCONST);
			isMatch = true;
		}

		if (!isMatch && !isOptional) {
			throw new Report.Error(lexAn.peekToken(), "Expected any of tokens: INTCONST, CHARCONST, STRINGCONST");
		}
	}

	/*
	 * Metode parseAssign, parseVal in parseAdds predstavljajo
	 * implementacijo sintaksnega analizatorja za gramatiko
	 *
	 * assign -> ID ASSIGN val .
	 * val -> INTCONST ops .
	 * ops -> .
	 * ops -> ADD ops .
         * ops -> SUB ops .
	 * 
	 * Te produkcije _niso_ del gramatike za PINS'24, ampak
	 * so namenjene zgolj in samo ilustraciji, kako se
	 * napise majhen sintaksni analizator.
	 */

	private void parseAssign() {
		switch (lexAn.peekToken().symbol()) {
		case IDENTIFIER:
			consume(Token.Symbol.IDENTIFIER);
			consume(Token.Symbol.ASSIGN);
			parseVal();
			return;
		default:
			throw new Report.Error(lexAn.peekToken(), "An identifier expected.");
		}
	}

	private void parseVal() {
		switch (lexAn.peekToken().symbol()) {
		case INTCONST:
			consume(Token.Symbol.INTCONST);
			parseAdds();
			return;
		default:
			throw new Report.Error(lexAn.peekToken(), "An integer constant expected.");
		}
	}

	private void parseAdds() {
		switch (lexAn.peekToken().symbol()) {
		case ADD:
			consume(Token.Symbol.ADD);
			parseAdds();
			return;
		case SUB:
			consume(Token.Symbol.SUB);
			parseAdds();
			return;
		case EOF:
			return;
		default:
			throw new Report.Error(lexAn.peekToken(), "An operator expected.");
		}
	}

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse();
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
