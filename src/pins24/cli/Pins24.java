package pins24.cli;

import pins24.common.PDM;
import pins24.common.Report;
import pins24.phase.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Pins24 {
    public static void main(final String[] cmdLineArgs) {
        try {
            if (cmdLineArgs.length == 0)
                throw new Report.Error("No source file specified in the command line.");
            if (cmdLineArgs.length > 1)
                Report.warning("Unused arguments in the command line.");

            try (SynAn synAn = new SynAn(readLinkedSourceFile(cmdLineArgs[0]))) {
                final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
                final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
                final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
                final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

                final List<PDM.CodeInstr> codeSegment = (new CodeGen.CodeSegmentGenerator(codegenAttrAST))
                        .codeSegment();
                final List<PDM.DataInstr> dataSegment = (new CodeGen.DataSegmentGenerator(codegenAttrAST))
                        .dataSegment();

                new Machine.Executor(codeSegment, dataSegment);
            }

        } catch (Report.Error error) {
            // Izpis opisa napake.
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    /**
     * Combines both the user source file and the standard library source file.
     */
    private static Reader readLinkedSourceFile(String srcFilePath) {
        List<Reader> readers = new ArrayList<>();
        readers.add(getFileReader(srcFilePath));
        // Append stdlib to the end of the source file,
        // so that user source file position info doesn't change.
        readers.add(getFileReader("./src/pins24/stdlib.pins24"));
        return new CombinedReader(readers);
    }

    private static Reader getFileReader(String filePath) {
        BufferedReader srcFileReader;
        try {
            srcFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
        } catch (FileNotFoundException __) {
            throw new Report.Error("File '" + filePath + "' not found.");
        }
        return srcFileReader;
    }

    private static class CombinedReader extends Reader {

        private final List<Reader> readers;
        private int activeReaderIndex;

        public CombinedReader(List<Reader> readers) {
            this.readers = readers;
            this.activeReaderIndex = 0;
        }

        @Override
        public int read(char[] charBuff, int off, int len) throws IOException {
            Reader activeReader = this.readers.get(this.activeReaderIndex);
            int charsRead = activeReader.read(charBuff);

            boolean activeReaderIsAtEnd = charsRead == -1;
            if (!activeReaderIsAtEnd) {
                return charsRead;
            }

            boolean hasRemainingReaders = readers.size() > activeReaderIndex + 1;
            if (hasRemainingReaders) {
                activeReaderIndex++;
                return this.read(charBuff, off, len);
            } else {
                return -1;
            }
        }

        @Override
        public void close() throws IOException {
            for (Reader reader : readers) {
                reader.close();
            }
        }
    }
}
