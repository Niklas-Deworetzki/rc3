package rc3.rssa;

import picocli.CommandLine;
import rc3.januscompiler.Direction;
import rc3.lib.Application;
import rc3.lib.Timers;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.parsing.Sink;
import rc3.rssa.instances.Program;
import rc3.rssa.vm.RSSAVM;
import rc3.rssa.vm.debug.REPL;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static picocli.CommandLine.*;

/**
 * This class represents the (main) entry point to the RSSA VM.
 * It is strongly adapted from the {@link rc3.januscompiler.Main} class of the Janus compiler.
 */
@CommandLine.Command(name = "rvm",

        synopsisHeading = "%n",
        abbreviateSynopsis = true,

        descriptionHeading = "%n",
        description = "This RSSA Virtual Machine is a research program used to read and execute Reversible Static Single Assignment code.",

        parameterListHeading = "%nParameters:%n",

        optionListHeading = "%nOptions:%n",
        sortOptions = false,

        footerHeading = "%n",
        footer = "Copyright (C) 2021 Technische Hochschule Mittelhessen, University of Applied Sciences, Dept. MNI",

        usageHelpAutoWidth = true)
public class Main extends Application {

    public static final int PICOCLI_ORDER_DEBUG = 100;

    @Parameters(index = "0", paramLabel = "INPUT",
            description = "The source file, that is used as an input for this program.")
    public File inputFile;

    @Parameters(index = "1", arity = "0..1", paramLabel = "OUTPUT",
            description = "An optional output file, where results are written to. " +
                    "If no file is specified, results are written to stdout instead.")
    public File outputFile = null; // Defaults to null (meaning stdout should be used).

    @Option(names = "--print-main",
            description = "Print the output variables of the 'main' subroutine after execution. This way the result " +
                    "of a program can be shown to the user, without the need of IO operations.%n")
    public boolean shouldPrintMain;

    @Option(names = {"--debug", "-d"},
            description = "Starts the debugger cli to debug interactive rssa code.")
    public boolean debug;

    @Option(names = {"--bw"},
            description = "The VM starts the execution of the RSSA code backwards, if the vm is selected.")
    public boolean backwardExecution = false;

    @Option(names = "--count-instructions",
            description = "Print the number of executed instructions after program execution.")
    public boolean countInstructions;

    @Option(names = "-D",
            description = "Enables diagnostic output during the compilers execution.")
    public boolean diagnosticEnabled = false;


    @ArgGroup(validate = false,
            order = PICOCLI_ORDER_DEBUG,
            heading = "%nDebug options:%n")
    public RSSAReader.DumpOptions dumpOptions = new RSSAReader.DumpOptions();


    public static final String
            TIME_SYNTAX = "time.syntax",
            TIME_SEMANTIC = "time.semantic",
            TIME_EXECUTION = "time.execution",
            TIME_TOTAL = "time.total";

    private final Timers timers = new Timers();
    private RSSAReader reader;
    private RSSAVM vm;

    private static double roundToTwoDigits(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    @Override
    public void cleanup() { // TODO Should total time always print?
        timers.stop(TIME_TOTAL);
        if (reader != null) this.timers.include(reader.getTimers());

        if (diagnosticEnabled) {
            if (timers.contains(TIME_SYNTAX)) {
                System.out.printf("Lexical and syntactic analysis %s ms\n",
                        timers.format(TIME_SYNTAX, TimeUnit.MILLISECONDS, 2));
            }
            if (timers.contains(TIME_SEMANTIC)) {
                System.out.printf("Semantic analysis %s ms\n",
                        timers.format(TIME_SEMANTIC, TimeUnit.MILLISECONDS, 2));
            }
            if (vm != null) {
                System.out.printf("Execution time %s ms\n",
                        timers.format(TIME_EXECUTION, TimeUnit.MILLISECONDS, 2));
                System.out.printf("Executed instructions %d \n",
                        vm.executedInstructions);

                double cyclesPerMs = (TimeUnit.NANOSECONDS.toMillis(1) * vm.executedInstructions) /
                        (double) timers.get(TIME_EXECUTION, TimeUnit.NANOSECONDS);
                System.out.printf("Execution rate %.2f instructions per ms\n",
                        roundToTwoDigits(cyclesPerMs));
            }
        }
        System.out.printf("Total time %s ms\n",
                timers.format(TIME_TOTAL, TimeUnit.MILLISECONDS, 2));
    }

    @Override
    public int run() throws ErrorMessage, IOException {
        timers.start(TIME_TOTAL);
        try (Sink sink = Sink.fromFileOrDefault(outputFile)) {
            this.reader = new RSSAReader(dumpOptions, inputFile);
            Optional<Program> program = reader.read();

            if (program.isPresent()) {
                Direction initalDirection = backwardExecution ? Direction.BACKWARD : Direction.FORWARD;
                RSSAVM vm = new RSSAVM(program.get(), initalDirection, sink.getOutputStream(),
                        shouldPrintMain, countInstructions);

                timers.time(TIME_EXECUTION, () -> {
                    this.vm = vm;
                    if (debug) {
                        new REPL(vm).run();
                    } else {
                        vm.run();
                    }
                });
            }
        }
        return EXIT_SUCCESS;
    }

    public static void main(String[] args) {
        runApplication(args, new Main());
    }
}

