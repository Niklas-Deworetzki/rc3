package rc3.testsuite.results;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JanusVsRSSAResult extends TestResult {
    private final TestOutput targetOutput;
    private final TestOutput actualOutput;
    private final boolean isIdentical;
    private final double percentageCorrectness;
    private final Set<String> outputDifference;

    public JanusVsRSSAResult(File input, String option, TestOutput targetOutput, TestOutput actualOutput) {
        super(input, option);
        this.targetOutput = targetOutput;
        this.actualOutput = actualOutput;

        // compute results
        isIdentical = targetOutput.equals(actualOutput);
        outputDifference = new HashSet<>(actualOutput.getVariableOutput());
        outputDifference.removeAll(targetOutput.getVariableOutput());
        assert targetOutput.getVariableOutput().size() != 0;
        if (isIdentical) {
            percentageCorrectness = 1;
        } else if (actualOutput.getVariableOutput().size() == 0) {
            percentageCorrectness = 0;
        } else {
            percentageCorrectness = 1 - ((double) outputDifference.size() / actualOutput.getVariableOutput().size());
        }
    }

    @Override
    public String makeTestOutput() {
        if (isIdentical) {
            // test results are identical
            return String.format("Test Results for janus file %s are identical:\n" +
                            "================================================================================\n" +
                            "Both %s\n\n" +
                            "================================================================================\n",
                    formatFileName(37), targetOutput);
        } else {
            // test results are different
            if (targetOutput.getExit() == actualOutput.getExit()) {
                // exits are identical
                return String.format("Test Results for janus file %s are NOT identical:\n" +
                                "================================================================================\n" +
                                "Both exited with exit code %d.\n\n" +
                                "Janus interpreter output:\n%s" +
                                "--------------------------------------------------------------------------------\n" +
                                "RSSA virtual machine output difference:\n%s\n" +
                                "================================================================================\n",
                        formatFileName(33), actualOutput.getExit(), formatCollection(targetOutput.getVariableOutput()), formatCollection(outputDifference));
            } else {
                // exits are different
                return String.format("Test Results for janus file %s are NOT identical:\n" +
                                "================================================================================\n" +
                                "Janus interpreter %s\n" +
                                "--------------------------------------------------------------------------------\n" +
                                "RSSA virtual machine exited with exit code %d and output difference:\n%s\n" +
                                "================================================================================\n",
                        formatFileName(33), targetOutput, actualOutput.getExit(), formatCollection(outputDifference));
            }
        }
    }

    @Override
    public String makeSummary() {
        return String.format(" %-52.52s | %9s |   %6.2f%%   \n" +
                        "––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––\n",
                formatFileName(52), "    " + (isIdentical ? "X" : " ") + "    ", percentageCorrectness * 100);

    }


    private <T> String formatCollection(Collection<T> collection) {
        StringBuilder sb = new StringBuilder();
        collection.forEach(s -> sb.append(s).append('\n'));
        return sb.toString();
    }

    private String formatFileName(int length) {
        var name = getInput().toString();
        if (name.length() > length && length > 3) {
            return "..." + name.substring(name.length() - (length - 3));
        } else
            return name;
    }

    public boolean isIdentical() {
        return isIdentical;
    }

    public double getPercentageCorrectness() {
        return percentageCorrectness;
    }
}
