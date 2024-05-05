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
	private Token previous = null;
	private Token current = null;

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
		previous = current;
		current = lexAn.takeToken();
		if (current.symbol() != expectedSymbol) {
			throw new Report.Error(current, "Expected " + expectedSymbol + " got " + current.symbol() + " '" + current.lexeme() + "'");
		}
		System.out.println(current);
		return current;
	}

	private Token previous() {
		return previous;
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
		if (check(Token.Symbol.ASSIGN)) {
			consume(Token.Symbol.ASSIGN);
			statements = parseStatements();
		}
		return new AST.FunDef(identifier.lexeme(), parameters, statements);
	}

	private List<AST.ParDef> parseParameters() {
		List<AST.ParDef> parameters = new ArrayList<>();
		if (!check(Token.Symbol.IDENTIFIER)) {
			return parameters;
		}
		Token firstParameter = consume(Token.Symbol.IDENTIFIER);
		parameters.add(new AST.ParDef(firstParameter.lexeme()));
		do {
			if (check(Token.Symbol.COMMA)) {
				consume(Token.Symbol.COMMA);
				Token otherParameter = consume(Token.Symbol.IDENTIFIER);
				parameters.add(new AST.ParDef(otherParameter.lexeme()));
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
		if (check(Token.Symbol.ASSIGN)) {
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
		AST.Expr expr = parseConjunctionExpression(isOptional);
		while (match(Token.Symbol.OR)) {
			Token operator = previous();
			AST.Expr right = parseConjunctionExpression(isOptional);
			expr = new AST.BinExpr(tokenToBinExprOperator(operator), expr, right);
		}
		return expr;
	}

	private AST.Expr parseConjunctionExpression(boolean isOptional) {
		AST.Expr expr = parseComparisonExpression(isOptional);
		while (match(Token.Symbol.AND)) {
			Token operator = previous();
			AST.Expr right = parseComparisonExpression(isOptional);
			expr = new AST.BinExpr(tokenToBinExprOperator(operator), expr, right);
		}
		return expr;
	}

	private AST.Expr parseComparisonExpression(boolean isOptional) {
		AST.Expr left = parseAdditionExpression(isOptional);
        if (match(
				Token.Symbol.EQU,
				Token.Symbol.NEQ,
				Token.Symbol.GTH,
				Token.Symbol.LTH,
				Token.Symbol.GEQ,
				Token.Symbol.LEQ
		)) {
			Token operator = previous();
            AST.Expr right = parseAdditionExpression(isOptional);
			return new AST.BinExpr(tokenToBinExprOperator(operator), left, right);
        } else {
			return left;
		}
	}

	private AST.Expr parseAdditionExpression(boolean isOptional) {
		AST.Expr expr = parseMultiplicationExpression(isOptional);
		while (match(Token.Symbol.ADD, Token.Symbol.SUB)) {
			Token operator = previous();
			AST.Expr right = parseMultiplicationExpression(isOptional);
			expr = new AST.BinExpr(tokenToBinExprOperator(operator), expr, right);
		}
		return expr;
	}

	private AST.Expr parseMultiplicationExpression(boolean isOptional) {
		AST.Expr expr = parsePrefixExpression(isOptional);
		while (match(Token.Symbol.MUL, Token.Symbol.DIV, Token.Symbol.MOD)) {
			Token operator = previous();
			AST.Expr right = parsePrefixExpression(isOptional);
			expr = new AST.BinExpr(tokenToBinExprOperator(operator), expr, right);
		}
		return expr;
	}

	private AST.Expr parsePrefixExpression(boolean isOptional) {
		if (match(
				Token.Symbol.NOT,
				Token.Symbol.ADD,
				Token.Symbol.SUB,
				Token.Symbol.PTR
		)) {
			Token operator = previous();
			return new AST.UnExpr(tokenToPrefixUnExprOperator(operator), parsePrefixExpression(isOptional));
		} else {
			return parsePostfixExpression(isOptional);
		}
	}

	private AST.Expr parsePostfixExpression(boolean isOptional) {
        AST.Expr expr = parseConstOrGroupExpression(isOptional);
		if (match(Token.Symbol.PTR)) {
			return new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr);
		} else {
			return expr;
		}
	}

    private AST.Expr parseConstOrGroupExpression(boolean isOptional) {
        if (check(Token.Symbol.LPAREN)) {
            consume(Token.Symbol.LPAREN);
            AST.Expr expr = parseExpression(false);
            consume(Token.Symbol.RPAREN);
			return expr;
        } else if (check(Token.Symbol.IDENTIFIER)) {
			return parseFunctionCallOrVariableAccessExpression();
		} else {
            return parseConst(isOptional);
        }
    }

	private AST.NameExpr parseFunctionCallOrVariableAccessExpression() {
		Token identifier = consume(Token.Symbol.IDENTIFIER);
		if (check(Token.Symbol.LPAREN)) {
			consume(Token.Symbol.LPAREN);
			List<AST.Expr> arguments = parseArguments();
			consume(Token.Symbol.RPAREN);
			return new AST.CallExpr(identifier.lexeme(), arguments);
		} else {
			return new AST.VarExpr(identifier.lexeme());
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
		} while (check(Token.Symbol.COMMA));
		return initializers;
	}

	private AST.Init parseInitializer() {
		if (check(Token.Symbol.INTCONST)) {
			Token num = consume(Token.Symbol.INTCONST);
			if (check(Token.Symbol.MUL)) {
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
		if (match(
				Token.Symbol.INTCONST,
				Token.Symbol.CHARCONST,
				Token.Symbol.STRINGCONST
		)) {
			Token constant = previous();
			return new AST.AtomExpr(tokenToAtomExprType(constant), constant.lexeme());
		}

		if (isOptional) {
			return null;
		} else {
			// TODO: Reuse the standard error message
			throw new Report.Error(lexAn.peekToken(), "Expected a constant got " + previous().symbol());
		}
	}

	private AST.AtomExpr.Type tokenToAtomExprType(Token token) {
		Map<Token.Symbol, AST.AtomExpr.Type> symbolToType = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.INTCONST, AST.AtomExpr.Type.INTCONST),
				new AbstractMap.SimpleEntry<>(Token.Symbol.CHARCONST, AST.AtomExpr.Type.CHRCONST),
				new AbstractMap.SimpleEntry<>(Token.Symbol.STRINGCONST, AST.AtomExpr.Type.STRCONST)
		);
		return symbolToType.get(token.symbol());
	}

	private AST.BinExpr.Oper tokenToBinExprOperator(Token token) {
		Map<Token.Symbol, AST.BinExpr.Oper> symbolToOper = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.OR, AST.BinExpr.Oper.OR),
				new AbstractMap.SimpleEntry<>(Token.Symbol.AND, AST.BinExpr.Oper.AND),
				new AbstractMap.SimpleEntry<>(Token.Symbol.ADD, AST.BinExpr.Oper.ADD),
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
		return symbolToOper.get(token.symbol());
	}

	private AST.UnExpr.Oper tokenToPrefixUnExprOperator(Token token) {
		Map<Token.Symbol, AST.UnExpr.Oper> symbolToOper = Map.ofEntries(
				new AbstractMap.SimpleEntry<>(Token.Symbol.ADD, AST.UnExpr.Oper.ADD),
				new AbstractMap.SimpleEntry<>(Token.Symbol.PTR, AST.UnExpr.Oper.MEMADDR),
				new AbstractMap.SimpleEntry<>(Token.Symbol.SUB, AST.UnExpr.Oper.SUB),
				new AbstractMap.SimpleEntry<>(Token.Symbol.NOT, AST.UnExpr.Oper.NOT)
		);
		return symbolToOper.get(token.symbol());
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
