package rc3.testsuite.results;

import java.io.File;

abstract public class TestResult {
    private final File input;
    private final String option;


    public TestResult(File input, String option) {
        this.input = input;
        this.option = option;
    }

    public File getInput() {
        return input;
    }


    public String getOption() {
        return option;
    }

    public abstract String makeTestOutput();

    public abstract String makeSummary();
}
