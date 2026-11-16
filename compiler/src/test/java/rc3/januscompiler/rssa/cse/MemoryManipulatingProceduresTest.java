package rc3.januscompiler.rssa.cse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import rc3.januscompiler.backend.rssa.RSSABackend;
import rc3.januscompiler.parse.Scanner;
import rc3.januscompiler.parse.symbol.JanusSymbolFactory;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.parse.Parser;
import rc3.januscompiler.pass.AliasingAnalysisPass;
import rc3.januscompiler.pass.GlobalAnalysisPass;
import rc3.januscompiler.pass.LocalAnalysisPass;
import rc3.januscompiler.pass.Pass;
import rc3.rssa.instances.Program;
import rc3.rssa.translation.MemoryManagement;
import rc3.rssa.translation.RSSAGenerator;
import rc3.rssa.translation.StatementRSSAGenerator;
import rc3.lib.parsing.WithCUP;
import rc3.lib.parsing.WithJFlex;
import rc3.lib.parsing.Source;
import rc3.rssa.cse.MemoryManipulatingProcedures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryManipulatingProceduresTest implements WithJFlex, WithCUP {

    private Program loadRSSAProgram(File file) throws FileNotFoundException {
        final Scanner scanner = createScanner(Scanner::new, Source.fromFileOrDefault(file));
        final Parser parser = new Parser(scanner, new JanusSymbolFactory());
        final rc3.januscompiler.syntax.Program program = (rc3.januscompiler.syntax.Program) parse(parser);

        final Environment environment = new Environment();

        // Execute all passes in specified order
        for (Pass pass : List.of(
                new GlobalAnalysisPass(),
                new LocalAnalysisPass(),
                new AliasingAnalysisPass())) {
            pass.execute(null, environment, program);
        }

        final MemoryManagement memoryManagement = new MemoryManagement(RSSABackend.DEFAULT_STACK_SIZE, RSSABackend.DEFAULT_HEAP_SIZE);
        return RSSAGenerator.generateProgramCode(StatementRSSAGenerator.ReversingConditionalGenerator::new, program, memoryManagement, environment);
    }


    private static final File TEST_FILE_DIRECTORY = new File("programs/tests/memory-manipulation/");

    private DynamicTest expectedProcedures(String program, String... expectedProcedureNames) {
        return DynamicTest.dynamicTest(program, () -> {
            // Load RSSA program.
            final Program rssaProgram = assertDoesNotThrow(() ->
                    loadRSSAProgram(new File(TEST_FILE_DIRECTORY, program)));

            // Run analysis
            final Map<String, Boolean> procedures = MemoryManipulatingProcedures.detectProcedures(rssaProgram);

            final Set<String> expectedProcedures, actualProcedures = new HashSet<>();
            expectedProcedures = new HashSet<>(Arrays.asList(expectedProcedureNames));

            for (Map.Entry<String, Boolean> entry : procedures.entrySet()) {
                if (entry.getValue()) {
                    actualProcedures.add(entry.getKey());
                }
            }

            assertEquals(expectedProcedures, actualProcedures);
        });
    }

    @TestFactory
    @DisplayName("Testing dataflow analysis for procedures that manipulate memory")
    public List<DynamicTest> testPrograms() {
        return List.of(
                expectedProcedures("test1.ja", "a", "b", "c", "d", "main"),
                expectedProcedures("test2.ja", "b", "c", "d", "main"),
                expectedProcedures("test3.ja")
        );
    }
}