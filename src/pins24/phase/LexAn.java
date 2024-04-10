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
		while (true) {
			switch (buffChar) {
				case '\n':
				case ' ':
				case '\t':
				case '\r':
					nextChar();
					break;
				case '-':
				case '+':
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
					readNumberToken();
					return;
				case -1:
					return;
				default:
					// Not yet implemented, remove later
					nextChar();
					Report.info("Unknown token: " + (char)buffChar + " (" + buffChar + ")");
			}
		}
	}

	private void readNumberToken() {
		StringBuilder lexeme = new StringBuilder();
        do {
            lexeme.append((char) buffChar);
            nextChar();
        } while (matchesPattern("[0-9]", buffChar));
		this.buffToken = new Token(getCurrentLocation(), Token.Symbol.INTCONST, lexeme.toString());
	}

	private Report.Location getCurrentLocation() {
		return new Report.Location(this.buffCharLine, this.buffCharColumn);
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
			System.exit(1);
		}
	}

}
