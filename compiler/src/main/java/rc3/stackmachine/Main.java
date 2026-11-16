package rc3.stackmachine;

import picocli.CommandLine;
import rc3.lib.Application;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.RSSAReader;
import rc3.rssa.pass.StackAllocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class Main extends Application {
    @CommandLine.Parameters(index = "0", paramLabel = "INPUT", arity = "1",
            description = "")
    public String inputFile;
    @CommandLine.Parameters(index = "1", paramLabel = "OUTPUT", arity = "0..1",
            description = "")
    public String outputFile = "out.rsc";

    @CommandLine.ArgGroup(validate = false,
            heading = "%nDebug options:%n")
    public RSSAReader.DumpOptions dumpOptions = new RSSAReader.DumpOptions();

    @Override
    public int run() throws ErrorMessage, IOException {
        try (PrintStream out = new PrintStream(new FileOutputStream(outputFile))) {
            RSSAReader reader = new RSSAReader(dumpOptions, new File(inputFile));
            reader.read().ifPresent(program -> {
                StackAllocation.allocateStackFrames(program);
                Translation.ofProgram(program, line ->
                        out.println(line.getRepresentation()));
            });
        }
        return 0;
    }

    public static void main(String[] args) {
        runApplication(args, new Main());
    }
}
