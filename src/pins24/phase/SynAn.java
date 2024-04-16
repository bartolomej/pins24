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
	public Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private void parseProgram() {
		parseAssign(); // TODO: nadomesti z ustrezno metodo
		return;
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
			check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.ASSIGN);
			parseVal();
			return;
		default:
			throw new Report.Error(lexAn.peekToken(), "An identifier expected.");
		}
	}

	private void parseVal() {
		switch (lexAn.peekToken().symbol()) {
		case INTCONST:
			check(Token.Symbol.INTCONST);
			parseAdds();
			return;
		default:
			throw new Report.Error(lexAn.peekToken(), "An integer constant expected.");
		}
	}

	private void parseAdds() {
		switch (lexAn.peekToken().symbol()) {
		case ADD:
			check(Token.Symbol.ADD);
			parseAdds();
			return;
		case SUB:
			check(Token.Symbol.SUB);
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
			System.exit(1);
		}
	}

}
