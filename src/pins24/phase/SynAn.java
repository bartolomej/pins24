package pins24.phase;

import pins24.common.*;

import java.io.*;
import java.util.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
	private HashMap<AST.Node, Report.Locatable> attrLoc;
	private Token current = null;

	/**
	 * Ustvari nov sintaksni analizator.
	 *
	 * @param srcFileName ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
        try {
            this.lexAn = new LexAn(new BufferedReader(new InputStreamReader(new FileInputStream(srcFileName))));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

	public SynAn(final Reader reader) {
		this.lexAn = new LexAn(reader);
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
		current = lexAn.takeToken();
		if (current.symbol() != expectedSymbol) {
			throw new Report.Error(current, "Expected " + expectedSymbol + " got " + current.symbol() + " '" + current.lexeme() + "'");
		}
		return current;
	}

	private boolean match(Token.Symbol... expectedSymbols) {
		for (Token.Symbol symbol : expectedSymbols) {
			if (check(symbol)) {
				consume(symbol);
				return true;
			}
		}
		return false;
	}

	private boolean check(Token.Symbol expectedSymbol) {
		return lexAn.peekToken().symbol() == expectedSymbol;
	}

	/**
	 * Uses the position of the next token as the start position for the current AST node.
	 */
	private Report.Locatable nextPosition() {
		return lexAn.peekToken().location();
	}

	private Report.Locatable currentPosition() {
		return current.location();
	}

	/**
	 * Uses the position of the previous token as the end position for the current AST node
	 * and returns the provided AST node for convenience (so that we don't have to use temporary variables).
	 */
	private <T extends AST.Node> T saveNodeRangeAndReturn(Report.Locatable startPosition, T node) {
		this.attrLoc.put(node, rangeFromBoundaryPositions(startPosition, currentPosition()));
		return node;
	}

	private Report.Locatable rangeFromBoundaryPositions(Report.Locatable startPosition, Report.Locatable endPosition) {
		return new Report.Location(
				startPosition.location().begLine(),
				startPosition.location().begColumn(),
				endPosition.location().endLine(),
				endPosition.location().endColumn()
		);
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> definitions = parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			throw new Report.Error(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return definitions;
	}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private AST.Nodes<AST.MainDef> parseProgram() {
		Report.Locatable startPosition = nextPosition();
		return saveNodeRangeAndReturn(startPosition, new AST.Nodes<>(parseDefinitions()));
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
		Report.Locatable startPosition = nextPosition();
		consume(Token.Symbol.FUN);
		Token identifier = consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.LPAREN);
		List<AST.ParDef> parameters = parseParameters();
		consume(Token.Symbol.RPAREN);
		List<AST.Stmt> statements = new ArrayList<>();
		if (check(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			statements = parseStatements();
		}
		return saveNodeRangeAndReturn(startPosition, new AST.FunDef(identifier.lexeme(), parameters, statements));
	}

	// TODO: Can we simplify this logic using the new previous() helper?
	private List<AST.ParDef> parseParameters() {
		Report.Locatable startPosition = nextPosition();
		List<AST.ParDef> parameters = new ArrayList<>();
		if (!check(Token.Symbol.IDENTIFIER)) {
			return parameters;
		}
		Token firstParameter = consume(Token.Symbol.IDENTIFIER);
		parameters.add(saveNodeRangeAndReturn(startPosition, new AST.ParDef(firstParameter.lexeme())));
		do {
			if (check(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				startPosition = nextPosition();
				Token otherParameter = consume(Token.Symbol.IDENTIFIER);
				parameters.add(saveNodeRangeAndReturn(startPosition, new AST.ParDef(otherParameter.lexeme())));
			}
		} while (check(Token.Symbol.COMMA));
		return parameters;
	}

	private List<AST.Stmt> parseStatements() {
		List<AST.Stmt> statements = new ArrayList<>();
		do {
			if (check(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
			}
			statements.add(parseStatement());
		} while (check(Token.Symbol.COMMA));
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
		Report.Locatable startPosition = nextPosition();
		consume(Token.Symbol.IF);
		AST.Expr condition = parseExpression(false);
		consume(Token.Symbol.THEN);
		List<AST.Stmt> thenStatements = parseStatements();

		List<AST.Stmt> elseStatements = new ArrayList<>();
		if (check(Token.Symbol.ELSE)) {
			consume(Token.Symbol.ELSE);
			elseStatements = parseStatements();
		}

		consume(Token.Symbol.END);

		return saveNodeRangeAndReturn(startPosition, new AST.IfStmt(condition, thenStatements, elseStatements));
	}

	private AST.WhileStmt parseWhileStatement() {
		Report.Locatable startPosition = nextPosition();
		consume(Token.Symbol.WHILE);
		AST.Expr condition = parseExpression(false);
		consume(Token.Symbol.DO);
		List<AST.Stmt> statements = parseStatements();
		consume(Token.Symbol.END);
		return saveNodeRangeAndReturn(startPosition, new AST.WhileStmt(condition, statements));
	}

	private AST.LetStmt parseLetStatement() {
		Report.Locatable startPosition = nextPosition();
		consume(Token.Symbol.LET);
		List<AST.MainDef> definitions = parseDefinitions();
		consume(Token.Symbol.IN);
		List<AST.Stmt> statements = parseStatements();
		consume(Token.Symbol.END);
		return saveNodeRangeAndReturn(startPosition, new AST.LetStmt(definitions, statements));
	}

	private AST.Stmt parseExpressionOrAssignmentStatement() {
		Report.Locatable startPosition = nextPosition();
		AST.Expr destinationExpression = parseExpression(false);
		if (check(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			AST.Expr sourceExpression = parseExpression(false);
			return saveNodeRangeAndReturn(startPosition, new AST.AssignStmt(destinationExpression, sourceExpression));
		} else {
			return saveNodeRangeAndReturn(startPosition, new AST.ExprStmt(destinationExpression));
		}
	}

	private AST.Expr parseExpression(boolean isOptional) {
		return parseDisjunctionExpression(isOptional);
	}

	private AST.Expr parseDisjunctionExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr expr = parseConjunctionExpression(isOptional);
		while (match(Token.Symbol.OR)) {
			Token operator = current;
			AST.Expr right = parseConjunctionExpression(isOptional);
			expr = saveNodeRangeAndReturn(startPosition, new AST.BinExpr(tokenToBinExprOperator(operator), expr, right));
		}
		return saveNodeRangeAndReturn(startPosition, expr);
	}

	private AST.Expr parseConjunctionExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr expr = parseComparisonExpression(isOptional);
		while (match(Token.Symbol.AND)) {
			Token operator = current;
			AST.Expr right = parseComparisonExpression(isOptional);
			expr = saveNodeRangeAndReturn(startPosition, new AST.BinExpr(tokenToBinExprOperator(operator), expr, right));
		}
		return saveNodeRangeAndReturn(startPosition, expr);
	}

	private AST.Expr parseComparisonExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr left = parseAdditionExpression(isOptional);
        if (match(
				Token.Symbol.EQU,
				Token.Symbol.NEQ,
				Token.Symbol.GTH,
				Token.Symbol.LTH,
				Token.Symbol.GEQ,
				Token.Symbol.LEQ
		)) {
			Token operator = current;
            AST.Expr right = parseAdditionExpression(isOptional);
			return saveNodeRangeAndReturn(startPosition, new AST.BinExpr(tokenToBinExprOperator(operator), left, right));
        } else {
			return saveNodeRangeAndReturn(startPosition, left);
		}
	}

	private AST.Expr parseAdditionExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr expr = saveNodeRangeAndReturn(startPosition, parseMultiplicationExpression(isOptional));
		while (match(Token.Symbol.ADD, Token.Symbol.SUB)) {
			Token operator = current;
			AST.Expr right = parseMultiplicationExpression(isOptional);
			expr = saveNodeRangeAndReturn(startPosition, new AST.BinExpr(tokenToBinExprOperator(operator), expr, right));
		}
		return saveNodeRangeAndReturn(startPosition, expr);
	}

	private AST.Expr parseMultiplicationExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr expr = parsePrefixExpression(isOptional);
		while (match(Token.Symbol.MUL, Token.Symbol.DIV, Token.Symbol.MOD)) {
			Token operator = current;
			AST.Expr right = parsePrefixExpression(isOptional);
			expr = saveNodeRangeAndReturn(startPosition, new AST.BinExpr(tokenToBinExprOperator(operator), expr, right));
		}
		return saveNodeRangeAndReturn(startPosition, expr);
	}

	private AST.Expr parsePrefixExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		if (match(
				Token.Symbol.NOT,
				Token.Symbol.ADD,
				Token.Symbol.SUB,
				Token.Symbol.PTR
		)) {
			Token operator = current;
			return saveNodeRangeAndReturn(startPosition, new AST.UnExpr(tokenToPrefixUnExprOperator(operator), parsePrefixExpression(isOptional)));
		} else {
			return saveNodeRangeAndReturn(startPosition, parsePostfixExpression(isOptional));
		}
	}

	private AST.Expr parsePostfixExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		AST.Expr expr = parseConstOrGroupExpression(isOptional);
		while (match(Token.Symbol.PTR)) {
			expr = saveNodeRangeAndReturn(startPosition, new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr));
		}
		return expr;
	}

    private AST.Expr parseConstOrGroupExpression(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		if (check(Token.Symbol.LPAREN)) {
            consume(Token.Symbol.LPAREN);
            AST.Expr expr = parseExpression(false);
            consume(Token.Symbol.RPAREN);
			return saveNodeRangeAndReturn(startPosition, expr);
        } else if (check(Token.Symbol.IDENTIFIER)) {
			return saveNodeRangeAndReturn(startPosition, parseFunctionCallOrVariableAccessExpression());
		} else {
            return saveNodeRangeAndReturn(startPosition, parseConst(isOptional));
        }
    }

	private AST.NameExpr parseFunctionCallOrVariableAccessExpression() {
		Report.Locatable startPosition = nextPosition();
		Token identifier = consume(Token.Symbol.IDENTIFIER);
		if (check(Token.Symbol.LPAREN)) {
			consume(Token.Symbol.LPAREN);
			List<AST.Expr> arguments = parseArguments();
			consume(Token.Symbol.RPAREN);
			return saveNodeRangeAndReturn(startPosition, new AST.CallExpr(identifier.lexeme(), arguments));
		} else {
			return saveNodeRangeAndReturn(startPosition, new AST.VarExpr(identifier.lexeme()));
		}
	}

	private List<AST.Expr> parseArguments() {
		List<AST.Expr> arguments = new ArrayList<>();
		AST.Expr firstArgument = parseExpression(true);
		if (firstArgument != null) {
			arguments.add(firstArgument);
		}
		do {
			if (check(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				arguments.add(parseExpression(false));
			}
		} while (!check(Token.Symbol.RPAREN));
		return arguments;
	}

	private AST.VarDef parseVarDefinition() {
		Report.Locatable startPosition = nextPosition();
		consume(Token.Symbol.VAR);
		Token name = consume(Token.Symbol.IDENTIFIER);
		consume(Token.Symbol.ASSIGN);
		List<AST.Init> initializers = parseInitializers();
		return saveNodeRangeAndReturn(startPosition, new AST.VarDef(name.lexeme(), initializers));
	}

	private List<AST.Init> parseInitializers() {
		List<AST.Init> initializers = new ArrayList<>();
		boolean isOptional = true;
		do {
			AST.Init initializer = parseInitializer(isOptional);
			if (initializer == null) {
				break;
			}
			initializers.add(initializer);
			isOptional = false;
		} while (match(Token.Symbol.COMMA));

		if (initializers.isEmpty()) {
			initializers.add(new AST.Init(
					new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"),
					new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "0")
			));
		}

		return initializers;
	}

	private AST.Init parseInitializer(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		if (check(Token.Symbol.INTCONST)) {
			Token num = consume(Token.Symbol.INTCONST);
			if (check(Token.Symbol.MUL)) {
				consume(Token.Symbol.MUL);
				AST.AtomExpr value = parseConst(false);
				return saveNodeRangeAndReturn(startPosition, new AST.Init(
						saveNodeRangeAndReturn(startPosition, new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, num.lexeme())),
						value
				));
			} else {
				return saveNodeRangeAndReturn(startPosition, new AST.Init(
                        saveNodeRangeAndReturn(startPosition, new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1")),
                        saveNodeRangeAndReturn(startPosition, new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, num.lexeme()))
                ));
			}
		} else {
			AST.AtomExpr value = parseConst(isOptional);
			if (value == null) {
				return null;
			}
			return saveNodeRangeAndReturn(startPosition, new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), value));
		}
	}

	private AST.AtomExpr parseConst(boolean isOptional) {
		Report.Locatable startPosition = nextPosition();
		if (match(
				Token.Symbol.INTCONST,
				Token.Symbol.CHARCONST,
				Token.Symbol.STRINGCONST
		)) {
			Token constant = current;
			return saveNodeRangeAndReturn(startPosition, new AST.AtomExpr(tokenToAtomExprType(constant), constant.lexeme()));
		}

		if (isOptional) {
			// TODO: Does this work as expected?
			return null;
		} else {
			// TODO: Reuse the standard error message
			throw new Report.Error(lexAn.peekToken(), "Expected a constant got " + lexAn.peekToken());
		}
	}

	private AST.AtomExpr.Type tokenToAtomExprType(Token token) {
		Map<Token.Symbol, AST.AtomExpr.Type> symbolToType = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.INTCONST, AST.AtomExpr.Type.INTCONST),
				new AbstractMap.SimpleEntry<>(Token.Symbol.CHARCONST, AST.AtomExpr.Type.CHRCONST),
				new AbstractMap.SimpleEntry<>(Token.Symbol.STRINGCONST, AST.AtomExpr.Type.STRCONST)
		);

		AST.AtomExpr.Type type = symbolToType.get(token.symbol());;

		if (type == null) {
			throw new Report.InternalError("No atom expression type mapping defined for token: " + token);
		} else {
			return type;
		}
	}

	private AST.BinExpr.Oper tokenToBinExprOperator(Token token) {
		Map<Token.Symbol, AST.BinExpr.Oper> symbolToOper = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.OR, AST.BinExpr.Oper.OR),
				new AbstractMap.SimpleEntry<>(Token.Symbol.AND, AST.BinExpr.Oper.AND),
				new AbstractMap.SimpleEntry<>(Token.Symbol.ADD, AST.BinExpr.Oper.ADD),
				new AbstractMap.SimpleEntry<>(Token.Symbol.SUB, AST.BinExpr.Oper.SUB),
				new AbstractMap.SimpleEntry<>(Token.Symbol.DIV, AST.BinExpr.Oper.DIV),
				new AbstractMap.SimpleEntry<>(Token.Symbol.MUL, AST.BinExpr.Oper.MUL),
				new AbstractMap.SimpleEntry<>(Token.Symbol.MOD, AST.BinExpr.Oper.MOD),
				new AbstractMap.SimpleEntry<>(Token.Symbol.EQU, AST.BinExpr.Oper.EQU),
				new AbstractMap.SimpleEntry<>(Token.Symbol.GEQ, AST.BinExpr.Oper.GEQ),
				new AbstractMap.SimpleEntry<>(Token.Symbol.GTH, AST.BinExpr.Oper.GTH),
				new AbstractMap.SimpleEntry<>(Token.Symbol.LEQ, AST.BinExpr.Oper.LEQ),
				new AbstractMap.SimpleEntry<>(Token.Symbol.LTH, AST.BinExpr.Oper.LTH),
				new AbstractMap.SimpleEntry<>(Token.Symbol.NEQ, AST.BinExpr.Oper.NEQ)
		);
		AST.BinExpr.Oper operator = symbolToOper.get(token.symbol());

		if (operator == null) {
			throw new Report.InternalError("No biary operator mapping defined for token: " + token);
		} else {
			return operator;
		}
	}

	private AST.UnExpr.Oper tokenToPrefixUnExprOperator(Token token) {
		Map<Token.Symbol, AST.UnExpr.Oper> symbolToOper = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.ADD, AST.UnExpr.Oper.ADD),
				new AbstractMap.SimpleEntry<>(Token.Symbol.PTR, AST.UnExpr.Oper.MEMADDR),
				new AbstractMap.SimpleEntry<>(Token.Symbol.SUB, AST.UnExpr.Oper.SUB),
				new AbstractMap.SimpleEntry<>(Token.Symbol.NOT, AST.UnExpr.Oper.NOT)
		);
		AST.UnExpr.Oper operator = symbolToOper.get(token.symbol());

		if (operator == null) {
			throw new Report.InternalError("No unary operator mapping defined for token: " + token);
		} else {
			return operator;
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
