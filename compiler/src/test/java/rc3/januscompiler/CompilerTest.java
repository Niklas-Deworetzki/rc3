package rc3.januscompiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import rc3.lib.DumpOptions;
import rc3.lib.optimization.OptimizationState;
import rc3.lib.messages.ErrorMessage;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompilerTest {

    private static final File TEST_DIR = new File("programs/tests");

    private static List<File> listInputFiles(String... path) {
        File containingFile = new File(CompilerTest.TEST_DIR, String.join(File.separator, path));
        File[] content = containingFile.listFiles();
        assertNotNull(content, String.format("No test files found in %s", containingFile.getAbsolutePath()));

        return Arrays.asList(content);
    }

    private static Compiler compilerForFile(File file) {
        return new Compiler(new DumpOptions(), false, null,
                new OptimizationState(),
                file, null);
    }

    @TestFactory
    @DisplayName("Semantic errors are detected")
    public Stream<DynamicTest> testSemanticErrors() {
        List<File> semanticErrors = listInputFiles("semantic-errors");
        return DynamicTest.stream(
                semanticErrors.iterator(),
                file -> String.format("Detects errors in %s", file.getName()),
                file -> Assertions.assertThrows(ErrorMessage.class,
                        () -> compilerForFile(file).compile()));
    }

    @TestFactory
    @DisplayName("Aliasing errors are detected")
    public Stream<DynamicTest> testAliasingErrors() {
        List<File> semanticErrors = listInputFiles("aliasing-errors");
        return DynamicTest.stream(
                semanticErrors.iterator(),
                file -> String.format("Detects errors in %s", file.getName()),
                file -> Assertions.assertThrows(ErrorMessage.class,
                        () -> compilerForFile(file).compile()));
    }
}