package pins24.phase;

import pins24.common.*;

import java.util.Arrays;
import java.util.List;

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
		System.out.println(token);
		return token;
	}

    /**
     * Returns `null` if `isOptional=true` and no token is consumed.
     */
    private Token consumeAnyOf(List<Token.Symbol> expectedSymbols, boolean isOptional) {
        for (Token.Symbol symbol : expectedSymbols) {
            if (match(symbol)) {
                return consume(symbol);
            }
        }

        if (isOptional) {
            return null;
        } else {
            String allSymbols = Arrays.toString(expectedSymbols.stream().map(Enum::toString).toArray());
            throw new Report.Error(lexAn.peekToken(), "Expected any of tokens " + allSymbols + " got " + lexAn.peekToken().symbol());
        }
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
				case FUN -> parseFunctionDefinition();
				case VAR -> parseVarDefinition();
				default -> {
					return;
				}
			}
		}
	}

	private void parseFunctionDefinition() {
		consume(Token.Symbol.FUN);
		consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.LPAREN);
		parseParameters();
		consume(Token.Symbol.RPAREN);
		if (match(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
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
			if (match(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
			}
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
		parseExpression(false);
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
		  parseExpression(false);
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
		parseExpression(false);
		if (match(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			parseExpression(false);
		}
	}

	private void parseExpression(boolean isOptional) {
		parseDisjunctionExpression(isOptional);
	}

	private void parseDisjunctionExpression(boolean isOptional) {
		parseConjunctionExpression(isOptional);
		if (match(Token.Symbol.OR)) {
			consume(Token.Symbol.OR);
			parseConjunctionExpression(isOptional);
		}
	}

	private void parseConjunctionExpression(boolean isOptional) {
		parseComparisonExpression(isOptional);
		if (match(Token.Symbol.AND)) {
			consume(Token.Symbol.AND);
			parseComparisonExpression(isOptional);
		}
	}

	private void parseComparisonExpression(boolean isOptional) {
		parseAdditionExpression(isOptional);
        Token comparisonToken = consumeAnyOf(Arrays.asList(
                Token.Symbol.EQU,
                Token.Symbol.NEQ,
                Token.Symbol.GTH,
                Token.Symbol.LTH,
                Token.Symbol.GEQ,
                Token.Symbol.LEQ
        ), true);
        if (comparisonToken != null) {
            parseAdditionExpression(isOptional);
        }
	}

	private void parseAdditionExpression(boolean isOptional) {
        parseMultiplicationExpression(isOptional);
        Token comparisonToken = consumeAnyOf(Arrays.asList(
                Token.Symbol.ADD,
                Token.Symbol.SUB
        ), true);
        if (comparisonToken != null) {
            parseMultiplicationExpression(isOptional);
        }
	}

	private void parseMultiplicationExpression(boolean isOptional) {
        parsePrefixExpression(isOptional);
        Token comparisonToken = consumeAnyOf(Arrays.asList(
                Token.Symbol.MUL,
                Token.Symbol.DIV,
                Token.Symbol.MOD
        ), true);
        if (comparisonToken != null) {
            parsePrefixExpression(isOptional);
        }
	}

	private void parsePrefixExpression(boolean isOptional) {
        Token prefixOperator = consumeAnyOf(Arrays.asList(
                Token.Symbol.NOT,
                Token.Symbol.ADD,
                Token.Symbol.SUB,
                Token.Symbol.PTR
        ), true);

		if (prefixOperator == null) {
			parsePostfixExpression(isOptional);
		} else {
			parsePrefixExpression(isOptional);
		}
	}

	private void parsePostfixExpression(boolean isOptional) {
        parseConstOrGroupExpression(isOptional);

        if (match(Token.Symbol.PTR)) {
            consume(Token.Symbol.PTR);
        }
	}

    private void parseConstOrGroupExpression(boolean isOptional) {
        if (match(Token.Symbol.LPAREN)) {
            consume(Token.Symbol.LPAREN);
            parseExpression(false);
            consume(Token.Symbol.RPAREN);
        } else if (match(Token.Symbol.IDENTIFIER)) {
			parseFunctionCallOrVariableAccessExpression();
		} else {
            parseConst(isOptional);
        }
    }

	private void parseFunctionCallOrVariableAccessExpression() {
		consume(Token.Symbol.IDENTIFIER);
		if (match(Token.Symbol.LPAREN)) {
			consume(Token.Symbol.LPAREN);
			parseArguments();
			consume(Token.Symbol.RPAREN);
		}
	}

	private void parseArguments() {
		parseExpression(true);
		do {
			if (match(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				parseExpression(false);
			}
		} while (!match(Token.Symbol.RPAREN));
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
		consumeAnyOf(Arrays.asList(
                Token.Symbol.INTCONST,
                Token.Symbol.CHARCONST,
                Token.Symbol.STRINGCONST
        ), isOptional);
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
