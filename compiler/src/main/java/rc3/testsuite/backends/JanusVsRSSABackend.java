package rc3.testsuite.backends;

import rc3.januscompiler.Main;
import rc3.testsuite.results.JanusVsRSSAResult;
import rc3.testsuite.results.TestOutput;
import rc3.testsuite.results.TestResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JanusVsRSSABackend extends TestBackend {
    private final boolean detailed;

    public JanusVsRSSABackend(boolean detailed) {
        super();
        this.detailed = detailed;
    }

    private static TestOutput getTestOutput(String inputFile, String optimization, boolean isTarget) {
        int exit = -1;
        File output = null;

        try {
            output = File.createTempFile("rptest", ".out");

            List<String> programParameters = new ArrayList<>(List.of(inputFile, output.getAbsolutePath()));

            if (isTarget) {
                programParameters.addAll(List.of("--backend=interpreter", "--print-main"));
            } else {
                // TODO: test rssa or rrssa? Or let user choose?
                if (!optimization.equals("no")) {
                    programParameters.add("-f" + optimization);
                }
                programParameters.addAll(List.of("--backend=rssa", "-bvm", "--print-main"));
            }
            exit = new Main().execute(programParameters.toArray(String[]::new));

        } catch (IOException exception) {
            System.err.println("Could not create temporary file.");
        }

        return new TestOutput(exit, output);
    }

    private TestResult test(File input, String optimization) {
        return new JanusVsRSSAResult(input, optimization,
                getTestOutput(input.getAbsolutePath(), optimization, true),
                getTestOutput(input.getAbsolutePath(), optimization, false));
    }

    public void startTesting(PrintStream out) {
        getTestFiles().forEach((optimization, files) -> {
            if (!files.isEmpty()) {
                System.out.printf("Testing %s optimization ...\n", optimization);

                files.forEach(file -> {
                    if (file.isDirectory()) {
                        File[] janusFiles = file.listFiles(pathname ->
                                pathname.getName().endsWith(".ja") && pathname.isFile());
                        // TODO: filter only .ja files in a directory or accept all files no matter their ending?
                        if (janusFiles != null)
                            for (File janusFile : janusFiles) {
                                addTestResult(optimization, test(janusFile, optimization));
                            }
                    } else {
                        // TODO: only accept files with .ja ending?
                        addTestResult(optimization, test(file, optimization));
                    }
                });
            }
        });
        getTestResults().forEach((opt, results) -> out.print(makeOutput(opt, results)));

    }

    private String getHeader(String opt) {
        return "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n" +
                "Test Results of " + opt + " optimization:\n" +
                "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";
    }

    private String makeOutput(String opt, Collection<TestResult> results) {
        StringBuilder sb = new StringBuilder(getHeader(opt));
        if (detailed) {
            sb.append("DETAILS\n================================================================================\n");
            results.forEach(testResult -> sb.append(testResult.makeTestOutput()));
        }
        sb.append(makeSummary(results));
        return sb.toString();
    }

    private String makeSummary(Collection<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SUMMARY\n" +
                        "================================================================================\n" +
                        " %-52s | %9s | %4s \n" +
                        "––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––\n",
                "file", "identical", "correctness"));
        results.forEach(testResult -> sb.append(testResult.makeSummary()));
        double identicalSum = results.stream()
                .filter(testResult -> ((JanusVsRSSAResult) testResult).isIdentical()).count() / (double) results.size();
        double correctnessSum = results.stream()
                .mapToDouble(result -> ((JanusVsRSSAResult) result).getPercentageCorrectness()).sum() * 100 / results.size();
        sb.append(String.format(" %-52s |  %6.2f%%  |   %6.2f%%   \n" +
                "––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––\n", "average", identicalSum * 100, correctnessSum));
        return sb.toString();
    }
}
