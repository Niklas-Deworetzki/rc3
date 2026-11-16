package rc3.testsuite.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class TestOutput {
    private final int exit;
    private final List<String> output = new ArrayList<>();

    public TestOutput(int exit, File output) {
        this.exit = exit;
        try (Scanner scanner = new Scanner(output)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                this.output.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            output.delete();
        }
    }

    public int getExit() {
        return exit;
    }

    public List<String> getOutput() {
        return output;
    }

    public Set<String> getVariableOutput() {
        return output.stream().filter(s -> !(s.isBlank() || s.contains("at end of"))).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return String.format("exited with exit code %d and output: %s", exit, output.isEmpty() ? "none" : "\n" + String.join("\n", output));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof TestOutput)) {
            return false;
        }

        TestOutput t = (TestOutput) o;
        return getVariableOutput().equals(t.getVariableOutput())
                && exit == t.getExit();
    }
}
