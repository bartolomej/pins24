package pins24.phase;

import java.util.*;
import pins24.common.*;

/**
 * Izracun pomnilniske predstavitve.
 */
public class Memory {

	@SuppressWarnings({ "doclint:missing" })
	public Memory() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 *
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke.</li>
	 * </ol>
	 */
	public static class AttrAST extends SemAn.AttrAST {

		/** Atribut: klicni zapis funkcije. */
		public final Map<AST.FunDef, Mem.Frame> attrFrame;

		/** Atribut: dostop do parametra. */
		public final Map<AST.ParDef, Mem.RelAccess> attrParAccess;

		/** Atribut: dostop do spremenljivke. */
		public final Map<AST.VarDef, Mem.Access> attrVarAccess;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi izracuna
		 * pomnilniske predstavitve.
		 *
		 * @param attrAST       Abstraktno sintaksno drevo z dodanimi atributi
		 *                      semanticne analize.
		 * @param attrFrame     Attribut: klicni zapis funkcije.
		 * @param attrParAccess Attribut: dostop do parametra.
		 * @param attrVarAccess Attribut: dostop do spremenljivke.
		 */
		public AttrAST(final SemAn.AttrAST attrAST, final Map<AST.FunDef, Mem.Frame> attrFrame,
				final Map<AST.ParDef, Mem.RelAccess> attrParAccess, final Map<AST.VarDef, Mem.Access> attrVarAccess) {
			super(attrAST);
			this.attrFrame = attrFrame;
			this.attrParAccess = attrParAccess;
			this.attrVarAccess = attrVarAccess;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi izracuna
		 * pomnilniske predstavitve.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrFrame = attrAST.attrFrame;
			this.attrParAccess = attrAST.attrParAccess;
			this.attrVarAccess = attrAST.attrVarAccess;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			head.append(highlighted ? "\033[31m" : "");
			switch (node) {
			case final AST.FunDef funDef:
				Mem.Frame frame = attrFrame.get(funDef);
				head.append(" depth=" + frame.depth);
				head.append(" parsSize=" + frame.parsSize);
				head.append(" varsSize=" + frame.varsSize);
				break;
			case final AST.ParDef parDef: {
				Mem.RelAccess relAccess = attrParAccess.get(parDef);
				head.append(" offset=" + relAccess.offset);
				head.append(" size=" + relAccess.size);
				head.append(" depth=" + relAccess.depth);
				if (relAccess.inits != null)
					initsToString(relAccess.inits, head);
				break;
			}
			case final AST.VarDef varDef: {
				Mem.Access access = attrVarAccess.get(varDef);
				if (access != null)
					switch (access) {
					case final Mem.AbsAccess absAccess:
						head.append(" size=" + absAccess.size);
						if (absAccess.inits != null)
							initsToString(absAccess.inits, head);
						break;
					case final Mem.RelAccess relAccess:
						head.append(" offset=" + relAccess.offset);
						head.append(" size=" + relAccess.size);
						head.append(" depth=" + relAccess.depth);
						if (relAccess.inits != null)
							initsToString(relAccess.inits, head);
						break;
					default:
						throw new Report.InternalError();
					}
				break;
			}
			default:
				break;
			}
			head.append(highlighted ? "\033[30m" : "");
			return head.toString();
		}

		/**
		 * Pripravi znakovno predstavitev zacetne vrednosti spremenmljivke.
		 *
		 * @param inits Zacetna vrednost spremenljivke.
		 * @param head  Znakovno predstavitev zacetne vrednosti spremenmljivke.
		 */
		private void initsToString(final List<Integer> inits, final StringBuffer head) {
			head.append(" inits=");
			int numPrintedVals = 0;
			int valPtr = 1;
			for (int init = 0; init < inits.get(0); init++) {
				final int num = inits.get(valPtr++);
				final int len = inits.get(valPtr++);
				int oldp = valPtr;
				for (int n = 0; n < num; n++) {
					valPtr = oldp;
					for (int l = 0; l < len; l++) {
						if (numPrintedVals == 10) {
							head.append("...");
							return;
						}
						head.append((numPrintedVals > 0 ? "," : "") + inits.get(valPtr++));
						numPrintedVals++;
					}
				}
			}
		}

	}

	/**
	 * Opravi izracun pomnilniske predstavitve.
	 *
	 * @param semanAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                     pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z atributi po fazi pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST organize(SemAn.AttrAST semanAttrAST) {
		AttrAST attrAST = new AttrAST(semanAttrAST, new HashMap<AST.FunDef, Mem.Frame>(),
				new HashMap<AST.ParDef, Mem.RelAccess>(), new HashMap<AST.VarDef, Mem.Access>());
		(new MemoryOrganizer(attrAST)).organize();
		return attrAST;
	}

	/**
	 * Organizator pomnilniske predstavitve.
	 */
	private static class MemoryOrganizer {

        /**
         * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
         * predstavitve.
         */
        private final AttrAST attrAST;

        /**
         * Ustvari nov organizator pomnilniske predstavitve.
         *
         * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
         *                pomnilniske predstavitve.
         */
        public MemoryOrganizer(final AttrAST attrAST) {
            this.attrAST = attrAST;
        }

        /**
         * Sprozi nov izracun pomnilniske predstavitve.
         *
         * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
         * predstavitve.
         */
        public AttrAST organize() {
            attrAST.ast.accept(new MemoryVisitor(), null);
            return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrFrame),
                    Collections.unmodifiableMap(attrAST.attrParAccess),
                    Collections.unmodifiableMap(attrAST.attrVarAccess));
        }

        /**
         * Obiskovalec, ki izracuna pomnilnisko predstavitev.
         */
        private class MemoryVisitor implements AST.FullVisitor<Object, Object> {

            @SuppressWarnings({"doclint:missing"})
            public MemoryVisitor() {
            }

            /*** TODO ***/

        }

        /**
         * Izracuna vrednost celostevilske konstante.
         *
         * @param intAtomExpr Celostevilska konstanta.
         * @param loc         Lokacija celostevilske konstante.
         * @return Vrednost celostevilske konstante.
         */
        public static Integer decodeIntConst(final AST.AtomExpr intAtomExpr, final Report.Locatable loc) {
            try {
                return Integer.decode(intAtomExpr.value);
            } catch (NumberFormatException __) {
                throw new Report.Error(loc, "Illegal integer value.");
            }
        }

        /**
         * Izracuna vrednost znakovna konstante.
         *
         * @param chrAtomExpr Znakovna konstanta.
         * @param loc         Lokacija znakovne konstante.
         * @return Vrednost znakovne konstante.
         */
        public static Integer decodeChrConst(final AST.AtomExpr chrAtomExpr, final Report.Locatable loc) {
            switch (chrAtomExpr.value.charAt(1)) {
                case '\\':
                    switch (chrAtomExpr.value.charAt(2)) {
                        case 'n':
                            return 10;
                        case '\'':
                            return ((int) '\'');
                        case '\\':
                            return ((int) '\\');
                        default:
                            return 16 * (((int) chrAtomExpr.value.charAt(2)) - ((int) '0'))
                                    + (((int) chrAtomExpr.value.charAt(3)) - ((int) '0'));
                    }
                default:
                    return ((int) chrAtomExpr.value.charAt(1));
            }
        }

        /**
         * Izracuna vrednost konstantnega niza.
         *
         * @param strAtomExpr Konstantni niz.
         * @param loc         Lokacija konstantnega niza.
         * @return Vrendnost konstantega niza.
         */
        public static Vector<Integer> decodeStrConst(final AST.AtomExpr strAtomExpr, final Report.Locatable loc) {
            final Vector<Integer> value = new Vector<Integer>();
            for (int c = 1; c < strAtomExpr.value.length() - 1; c++) {
                switch (strAtomExpr.value.charAt(c)) {
                    case '\\':
                        switch (strAtomExpr.value.charAt(c + 1)) {
                            case 'n':
                                value.addLast(10);
                                c += 1;
                                break;
                            case '\"':
                                value.addLast((int) '\"');
                                c += 1;
                                break;
                            case '\\':
                                value.addLast((int) '\\');
                                c += 1;
                                break;
                            default:
                                value.addLast(16 * (((int) strAtomExpr.value.charAt(c + 1)) - ((int) '0'))
                                        + (((int) strAtomExpr.value.charAt(c + 2)) - ((int) '0')));
                                c += 2;
                                break;
                        }
                        break;
                    default:
                        value.addLast((int) strAtomExpr.value.charAt(c));
                        break;
                }
            }
            return value;
        }

        // --- ZAGON ---

        /**
         * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
         *
         * @param cmdLineArgs Argumenti v ukazni vrstici.
         */
        public static void main(final String[] cmdLineArgs) {
            System.out.println("This is PINS'24 compiler (memory):");

            try {
                if (cmdLineArgs.length == 0)
                    throw new Report.Error("No source file specified in the command line.");
                if (cmdLineArgs.length > 1)
                    Report.warning("Unused arguments in the command line.");

                try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
                    // abstraktna sintaksa:
                    final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
                    // semanticna analiza:
                    final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
                    // pomnilniska predstavitev:
                    final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);

                    (new AST.Logger(memoryAttrAST)).log();
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
}
