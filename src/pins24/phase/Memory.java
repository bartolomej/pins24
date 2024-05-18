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
		// Naslovi in cela stevila so 32b
		private final int ADDRESS_BYTE_SIZE = 4;
		private final int NUMBER_BYTE_SIZE = 4;

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
			private int currentDepth;

            @SuppressWarnings({"doclint:missing"})
            public MemoryVisitor() {
				this.currentDepth = 0;
            }


			@Override
			public Object visit(AST.VarDef varDef, Object arg) {
				Vector<Integer> inits = decodeInits(varDef);
				int initsSize = getInitsSizeInBytes(inits);
				if (currentDepth == 0) {
					attrAST.attrVarAccess.put(varDef, new Mem.AbsAccess(
							varDef.name,
							initsSize,
							inits
					));
				} else {
					attrAST.attrVarAccess.put(varDef, new Mem.RelAccess(
							0,
							currentDepth,
							initsSize,
							inits,
							varDef.name
					));
				}
				return AST.FullVisitor.super.visit(varDef, arg);
			}

			@Override
			public Object visit(AST.FunDef funDef, Object arg) {
				this.currentDepth++;

				int parsSize = 0;

				for (int i = 0; i < funDef.pars.size(); i++) {
					AST.ParDef parameter = funDef.pars.get(i);
					Vector<Integer> inits = new Vector<>();
					inits.add(0);
					int size = NUMBER_BYTE_SIZE;
					parsSize += size;
					attrAST.attrParAccess.put(parameter, new Mem.RelAccess(
							i * size,
							currentDepth,
							size,
							inits,
							parameter.name

					));
				}

				int staticLinkSize = ADDRESS_BYTE_SIZE;
				int varsSize = 0; // FIXME
				attrAST.attrFrame.put(funDef, new Mem.Frame(
						funDef.name,
						currentDepth,
						parsSize + staticLinkSize,
						varsSize,
						new ArrayList<>(),
						new ArrayList<>()

				));

				AST.FullVisitor.super.visit(funDef, arg);

				this.currentDepth--;

				return null;
			}

			@Override
			public Object visit(AST.ParDef parDef, Object arg) {
				return AST.FullVisitor.super.visit(parDef, arg);
			}

			@Override
			public Object visit(AST.LetStmt letStmt, Object arg) {
				return AST.FullVisitor.super.visit(letStmt, arg);
			}
		}

		/**
		 * Calculates the total size of initializers in bytes.
		 * @param inits The initializers array conforming to the rules in `decodeInits`.
		 * @return Number of bytes
		 */
		private int getInitsSizeInBytes(Vector<Integer> inits) {
			int totalSizeInBytes = 0;
			int i = 1;
			while (i < inits.size()) {
				int repetitionsOfNextElement = inits.get(i);
				int sizeOfElementGroup = inits.get(i + 1);
				totalSizeInBytes += repetitionsOfNextElement * sizeOfElementGroup * NUMBER_BYTE_SIZE;
				i += 2 + sizeOfElementGroup;
			}
			return totalSizeInBytes;
		}

		private Vector<Integer> decodeInits(AST.VarDef varDef) {
			final Vector<Integer> inits = new Vector<Integer>();

			// The very first element must indicate the number of total initializers.
			inits.add(varDef.inits.size());

			for (AST.Init init : varDef.inits) {
				int num = decodeConst(init.num).getFirst();

				// Indicate the number of repetitions of the next group of elements (many in case of string)
				inits.add(num);

				Vector<Integer> elementsGroup = decodeConst(init.value);
				inits.add(elementsGroup.size());
				inits.addAll(elementsGroup);
			}

			return inits;
		}

		private Vector<Integer> decodeConst(final AST.AtomExpr atomExpr) {
			final Vector<Integer> value = new Vector<Integer>();
			switch (atomExpr.type) {
				case CHRCONST -> value.add((int) atomExpr.value.charAt(0));
				case STRCONST -> {
					for (int c = 0; c < atomExpr.value.length(); c++) {
						value.addLast((int) atomExpr.value.charAt(c));
					}
				}
				case INTCONST -> {
					try {
						value.add(Integer.decode(atomExpr.value));
					} catch (NumberFormatException __) {
						throw new Report.Error(attrAST.attrLoc.get(atomExpr), "Illegal integer value.");
					}
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
