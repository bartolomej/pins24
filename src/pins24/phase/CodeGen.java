package pins24.phase;

import java.util.*;
import pins24.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
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
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame parentFrame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Mem.Frame frame = attrAST.attrFrame.get(funDef);
				Report.Locatable loc = attrAST.attrLoc.get(funDef);

				if (parentFrame == null) {
					// TODO: This is a global function, do we need to do anything extra?
				}

				instrs.add(new PDM.LABEL(funDef.name, loc));

				// Size of FP + RA
				int omittedPointerSizes = 8;
				if (frame.varsSize < omittedPointerSizes) {
					throw new Report.InternalError("Invalid vars size");
				}

				// Initialize the memory space for all the local variables (accepts a negative operand).
				// Note that `varsSize` includes the size of FP and RA.
				instrs.add(new PDM.PUSH(-(frame.varsSize - omittedPointerSizes), loc));
				instrs.add(new PDM.POPN(loc));


				// TODO: Do we need to do the same for params?
				instrs.addAll(funDef.stmts.accept(this, frame));

				// Note tat `parsSize` includes the size of SL
				instrs.add(new PDM.PUSH(frame.parsSize - 4, loc));
				instrs.add(new PDM.RETN(frame, loc));

				attrAST.attrCode.put(funDef, instrs);

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.Nodes<? extends AST.Node> nodes, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				for (final AST.Node node : nodes) {
					instrs.addAll(node.accept(this, frame));
				}
				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.ExprStmt exprStmt, Mem.Frame frame) {
				return exprStmt.expr.accept(this, frame);
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.CallExpr callExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(callExpr);

				instrs.addAll(callExpr.args.accept(this, frame));

				// Push static link
				instrs.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
				instrs.add(new PDM.LOAD(loc));

				instrs.add(new PDM.NAME(callExpr.name, loc));
				instrs.add(new PDM.CALL(frame, loc));

				attrAST.attrCode.put(callExpr, instrs);

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AtomExpr atomExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(atomExpr);

				Vector<Integer> values = Memory.decodeConst(atomExpr, attrAST);
				for (Integer value : values) {
					instrs.add(new PDM.PUSH(value, loc));
				}

				attrAST.attrCode.put(atomExpr, instrs);

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();

				instrs.addAll(letStmt.defs.accept(this, frame));
				instrs.addAll(letStmt.stmts.accept(this, frame));

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varDef);
				Mem.Access access = attrAST.attrVarAccess.get(varDef);

				switch (access) {
					case final Mem.RelAccess relAccess: {
						String dataLabelName = ":" + labelCounter;
						labelCounter++;

						// Prepare data instructions
						List<PDM.DataInstr> dataInstrs = new ArrayList<>();
						dataInstrs.add(new PDM.LABEL(dataLabelName, loc));
						List<Integer> decodedInits = Memory.decodeInits(varDef, attrAST);
						for (Integer decodedInit : decodedInits) {
							dataInstrs.add(new PDM.DATA(decodedInit, loc));
						}
						attrAST.attrData.put(varDef, dataInstrs);

						// Prepare code instructions
						instrs.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
						instrs.add(new PDM.PUSH(relAccess.offset, loc));
						instrs.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
						instrs.add(new PDM.NAME(dataLabelName, loc));
						instrs.add(new PDM.INIT(loc));
						break;
					}
					case final Mem.AbsAccess absAccess: {
						// TODO
						break;
					}
					default:
						throw new Report.InternalError("Unreachable");
				}

				attrAST.attrCode.put(varDef, instrs);

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AssignStmt assignStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(assignStmt);

				instrs.addAll(assignStmt.srcExpr.accept(this, frame));

				instrs.addAll(assignStmt.dstExpr.accept(this, frame));
				if (assignStmt.dstExpr instanceof AST.VarExpr) {
					// TODO: Find a better solution
					// The last command is LOAD,
					// but we need the variable address at the top of the stack here.
					instrs.removeLast();
				}
				instrs.add(new PDM.SAVE(loc));

				attrAST.attrCode.put(assignStmt, instrs);

				return instrs;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarExpr varExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> instrs = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varExpr);
				AST.Def def = attrAST.attrDef.get(varExpr);

				if (!(def instanceof AST.VarDef)) {
					throw new Report.InternalError("Unreachable");
				}

				Mem.Access access = attrAST.attrVarAccess.get(def);

				switch (access) {
					case final Mem.RelAccess relAccess: {
						instrs.add(new PDM.PUSH(relAccess.offset, loc));
						instrs.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
						instrs.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
						// TODO: Decide if we should load the value or not
						// See: https://discord.com/channels/370216420199628800/483365879082385428/1244290276294656072
						instrs.add(new PDM.LOAD(loc));
						break;
					}
					case final Mem.AbsAccess absAccess: {
						// TODO:
						break;
					}
					default:
						throw new Report.InternalError("Unreachable");
				}

				attrAST.attrCode.put(varExpr, instrs);

				return instrs;
			}
		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (code generation):");

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
				// generiranje kode:
				final AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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
