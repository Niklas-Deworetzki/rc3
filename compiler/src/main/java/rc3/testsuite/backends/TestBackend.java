package rc3.testsuite.backends;

import rc3.testsuite.results.TestResult;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TestBackend {
    private final Map<String, Set<File>> testFiles = new HashMap<>();
    private final Map<String, Set<TestResult>> testResults = new HashMap<>();

    public abstract void startTesting(PrintStream out);

    public void setTestFiles(String option, Set<File> files) {
        this.testFiles.put(option, files);
    }

    public Map<String, Set<File>> getTestFiles() {
        return testFiles;
    }

    public Map<String, Set<TestResult>> getTestResults() {
        return testResults;
    }

    public void addTestResult(String option, TestResult result) {
        testResults.putIfAbsent(option, new HashSet<>());
        testResults.get(option).add(result);
    }

    public void addTestResults(String option, Set<TestResult> results) {
        testResults.put(option, results);
    }
}
