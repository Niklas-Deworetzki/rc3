package rc3.testsuite;

import picocli.CommandLine;
import rc3.lib.Application;
import rc3.lib.messages.ErrorMessage;
import rc3.testsuite.backends.JanusVsRSSABackend;
import rc3.testsuite.backends.TestBackend;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

@CommandLine.Command(name = "rptest",

        synopsisHeading = "%n",
        abbreviateSynopsis = true,

        descriptionHeading = "%n",
        description = "This Reverse Programming Testsuite is a research program used to test the execution of reversible code.",

        parameterListHeading = "%nParameters:%n",

        optionListHeading = "%nOptions:%n",
        sortOptions = false,

        footerHeading = "%n",
        footer = "Copyright (C) 2021 Technische Hochschule Mittelhessen, University of Applied Sciences, Dept. MNI",

        usageHelpAutoWidth = true)
public class Main extends Application {

    public static void main(String[] args) {
        runApplication(args, new Main());
    }

    private static PrintStream openOutput(File outputFile) throws FileNotFoundException {
        if (outputFile != null) {
            return new PrintStream(new FileOutputStream(outputFile), true);
        } else {
            return System.out;
        }
    }

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "OUTPUT",
            description = "An optional output file, where results are written to. " +
                    "If no file is specified, results are written to stdout instead.")
    public File outputFile = null; // Defaults to null (meaning stdout should be used).

    // TODO: refactor optimization options in own class
    @CommandLine.Option(names = "-fcse", split = ",", description = "Tests cse optimization with the given list of files.")
    public Set<File> cseFiles = new HashSet<>();

    @CommandLine.Option(names = "-fcp", split = ",", description = "Tests cp optimization with the given list of files.")
    public Set<File> cpFiles = new HashSet<>();

    @CommandLine.Option(names = "-fstatic-jumps", split = ",", description = "Tests static-jumps optimization with the given list of files.")
    public Set<File> staticJumpsFiles = new HashSet<>();

    @CommandLine.Option(names = "-finline", split = ",", description = "Tests inline optimization with the given list of files.")
    public Set<File> inlineFiles = new HashSet<>();

    @CommandLine.Option(names = "-fno", split = ",", description = "Tests inline optimization with the given list of files.")
    public Set<File> noFiles = new HashSet<>();

    @CommandLine.Option(names = {"-d", "--detailed"}, description = "Prints detailed information about the test results.")
    public boolean detailed = false;

    /*
    @CommandLine.ArgGroup(validate = false,
            order = PICOCLI_ORDER_DEBUG,
            heading = "%nDebug options:%n")
    public RSSAReader.DumpOptions dumpOptions = new RSSAReader.DumpOptions();
    */

    @Override
    public int run() throws ErrorMessage, IOException, CommandLine.ParameterException {
        try (PrintStream printStream = openOutput(outputFile)) {
            System.out.println("Hello testsuite!");

            // TODO: choose backend
            TestBackend backend = new JanusVsRSSABackend(detailed);
            backend.setTestFiles("cse", cseFiles);
            backend.setTestFiles("cp", cpFiles);
            backend.setTestFiles("static-jumps", staticJumpsFiles);
            backend.setTestFiles("inline", inlineFiles);
            backend.setTestFiles("no", noFiles);

            backend.startTesting(printStream);
        }
        return EXIT_SUCCESS;
    }
}
