package pins24.phase;

import pins24.common.*;

import java.util.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
	private HashMap<AST.Node, Report.Locatable> attrLoc;

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
	 * @param expectedSymbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token consume(Token.Symbol expectedSymbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != expectedSymbol) {
			throw new Report.Error(token, "Expected " + expectedSymbol + " got " + token.symbol() + " '" + token.lexeme() + "'");
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
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> definitions = parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return definitions;
	}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private AST.Nodes<AST.MainDef> parseProgram() {
		return new AST.Nodes<>(parseDefinitions());
	}

	private List<AST.MainDef> parseDefinitions() {
		List<AST.MainDef> definitions = new ArrayList<>();
		while (true) {
			switch (lexAn.peekToken().symbol()) {
				case FUN -> {
					definitions.add(parseFunctionDefinition());
				}
				case VAR -> {
					definitions.add(parseVarDefinition());
				}
				default -> {
					return definitions;
				}
			}
		}
	}

	private AST.FunDef parseFunctionDefinition() {
		consume(Token.Symbol.FUN);
		Token identifier = consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.LPAREN);
		List<AST.ParDef> parameters = parseParameters();
		consume(Token.Symbol.RPAREN);
		List<AST.Stmt> statements = new ArrayList<>();
		if (match(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			statements = parseStatements();
		}
		return new AST.FunDef(identifier.lexeme(), parameters, statements);
	}

	private List<AST.ParDef> parseParameters() {
		List<AST.ParDef> parameters = new ArrayList<>();
		if (!match(Token.Symbol.IDENTIFIER)) {
			return parameters;
		}
		Token firstParameter = consume(Token.Symbol.IDENTIFIER);
		parameters.add(new AST.ParDef(firstParameter.lexeme()));
		do {
			if (match(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				Token otherParameter = consume(Token.Symbol.IDENTIFIER);
				parameters.add(new AST.ParDef(otherParameter.lexeme()));
			}
		} while (match(Token.Symbol.COMMA));
		return parameters;
	}

	private List<AST.Stmt> parseStatements() {
		List<AST.Stmt> statements = new ArrayList<>();
		do {
			if (match(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
			}
			statements.add(parseStatement());
		} while (match(Token.Symbol.COMMA));
		return statements;
	}

	private AST.Stmt parseStatement() {
		switch (lexAn.peekToken().symbol()) {
			case IF -> {
				return parseIfStatement();
			}
			case WHILE -> {
				return parseWhileStatement();
			}
			case LET -> {
				return parseLetStatement();
			}
			default -> {
				return parseExpressionOrAssignmentStatement();
			}
		}
	}

	private AST.IfStmt parseIfStatement() {
		consume(Token.Symbol.IF);
		AST.Expr condition = parseExpression(false);
		consume(Token.Symbol.THEN);
		List<AST.Stmt> thenStatements = parseStatements();

		List<AST.Stmt> elseStatements = new ArrayList<>();
		if (match(Token.Symbol.ELSE)) {
			consume(Token.Symbol.ELSE);
			elseStatements = parseStatements();
		}

		consume(Token.Symbol.END);

		return new AST.IfStmt(condition, thenStatements, elseStatements);
	}

	private AST.WhileStmt parseWhileStatement() {
		  consume(Token.Symbol.WHILE);
		  AST.Expr condition = parseExpression(false);
		  consume(Token.Symbol.DO);
		  List<AST.Stmt> statements = parseStatements();
		  consume(Token.Symbol.END);
		  return new AST.WhileStmt(condition, statements);
	}

	private AST.LetStmt parseLetStatement() {
		consume(Token.Symbol.LET);
		List<AST.MainDef> definitions = parseDefinitions();
		consume(Token.Symbol.IN);
		List<AST.Stmt> statements = parseStatements();
		consume(Token.Symbol.END);
		return new AST.LetStmt(definitions, statements);
	}

	private AST.Stmt parseExpressionOrAssignmentStatement() {
		AST.Expr destinationExpression = parseExpression(false);
		if (match(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			AST.Expr sourceExpression = parseExpression(false);
			return new AST.AssignStmt(destinationExpression, sourceExpression);
		} else {
			return new AST.ExprStmt(destinationExpression);
		}
	}

	private AST.Expr parseExpression(boolean isOptional) {
		return parseDisjunctionExpression(isOptional);
	}

	private AST.Expr parseDisjunctionExpression(boolean isOptional) {
		Token disjunctionToken;
		do {
			parseConjunctionExpression(isOptional);
			disjunctionToken = consumeAnyOf(List.of(Token.Symbol.OR), true);
		} while (disjunctionToken != null);
		return null; // TODO: Implement
	}

	private void parseConjunctionExpression(boolean isOptional) {
		Token conjunctionToken;
		do {
			parseComparisonExpression(isOptional);
			conjunctionToken = consumeAnyOf(List.of(Token.Symbol.AND), true);
		} while (conjunctionToken != null);
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
		Token comparisonToken;
		do {
        	parseMultiplicationExpression(isOptional);
			comparisonToken = consumeAnyOf(Arrays.asList(
					Token.Symbol.ADD,
					Token.Symbol.SUB
			), true);
		} while (comparisonToken != null);
	}

	private void parseMultiplicationExpression(boolean isOptional) {
		Token comparisonToken;
		do {
			parsePrefixExpression(isOptional);
			comparisonToken = consumeAnyOf(Arrays.asList(
					Token.Symbol.MUL,
					Token.Symbol.DIV,
					Token.Symbol.MOD
			), true);
		} while (comparisonToken != null);
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

	private AST.VarDef parseVarDefinition() {
		consume(Token.Symbol.VAR);
		Token name = consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.ASSIGN);
		List<AST.Init> initializers = parseInitializers();
		return new AST.VarDef(name.lexeme(), initializers);
	}

	private List<AST.Init> parseInitializers() {
		List<AST.Init> initializers = new ArrayList<>();
		do {
			initializers.add(parseInitializer());
		} while (match(Token.Symbol.COMMA));
		return initializers;
	}

	private AST.Init parseInitializer() {
		if (match(Token.Symbol.INTCONST)) {
			Token num = consume(Token.Symbol.INTCONST);
			if (match(Token.Symbol.MUL)) {
				consume(Token.Symbol.MUL);
				AST.AtomExpr value = parseConst(false);
				return new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, num.lexeme()), value);
			} else {
				return new AST.Init(
                        new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"),
                        new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, num.lexeme())
                );
			}
		} else {
			AST.AtomExpr value = parseConst(false);
			return new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), value);
		}
	}

	private AST.AtomExpr parseConst(boolean isOptional) {
		Token constant = consumeAnyOf(Arrays.asList(
                Token.Symbol.INTCONST,
                Token.Symbol.CHARCONST,
                Token.Symbol.STRINGCONST
        ), isOptional);

        // TODO: Do we need optionality?
        if (constant == null) {
            return null;
        } else {
            Map<Token.Symbol, AST.AtomExpr.Type> constSymbolToType = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>(Token.Symbol.INTCONST, AST.AtomExpr.Type.INTCONST),
                    new AbstractMap.SimpleEntry<>(Token.Symbol.CHARCONST, AST.AtomExpr.Type.CHRCONST),
                    new AbstractMap.SimpleEntry<>(Token.Symbol.STRINGCONST, AST.AtomExpr.Type.STRCONST)
            );
            return new AST.AtomExpr(constSymbolToType.get(constant.symbol()), constant.lexeme());
        }
	}

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

			final HashMap<AST.Node, Report.Locatable> attrLoc = new HashMap<AST.Node, Report.Locatable>();
			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse(attrLoc);
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
