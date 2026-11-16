package rc3.januscompiler;

import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.backend.interpreter.InterpreterBackend;
import rc3.januscompiler.backend.optimizer.LoopOptimizerPass;
import rc3.januscompiler.backend.rssa.RSSABackend;
import rc3.januscompiler.backend.tac.TacBackend;
import rc3.januscompiler.parse.Parser;
import rc3.januscompiler.parse.Scanner;
import rc3.januscompiler.parse.symbol.JanusSymbolFactory;
import rc3.januscompiler.pass.AliasingAnalysisPass;
import rc3.januscompiler.pass.GlobalAnalysisPass;
import rc3.januscompiler.pass.LocalAnalysisPass;
import rc3.januscompiler.pass.Pass;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.table.Environment;
import rc3.lib.DumpOptions;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.optimization.OptimizationState;
import rc3.lib.parsing.Sink;
import rc3.lib.parsing.Source;
import rc3.lib.parsing.WithCUP;
import rc3.lib.parsing.WithJFlex;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * This class provides a programmatic interface to all {@link Pass Passes} and {@link Backend Backends} as well as
 * {@link AvailableOptimization Optimizations} implemented as part of this framework.
 * <p>
 * When a {@link Compiler} is instantiated, a set of options is passed controlling the {@link Compiler Compilers}
 * behavior. To actually compile (or run) an input, the {@link Compiler#compile()} method is used.
 * </p>
 */
public class Compiler implements WithCUP, WithJFlex {

    /**
     * An enumeration listing the different {@link Backend Backends} that are available for this {@link Compiler}.
     * <p>
     * If a new {@link Backend} is developed, this list should be updated accordingly as well as the accompanying
     * {@link Compiler#instantiateBackend(AvailableBackend)} method, which is used to instantiate a {@link Backend}.
     * </p>
     * <p>
     * The backends listed here and their <var>cliName</var> are used by <i>picocli</i> to select a {@link Backend}
     * from user input. Since this class is an enum, it is possible for this library to show an user all available
     * backends, if a usage message is requested.
     * </p>
     */
    public enum AvailableBackend {
        TAC_BACKEND("tac"),
        INTERPRETER("interpreter"),
        RSSA_ORIGINAL("rssa"),
        RSSA_IMPROVED_CONTROL_FLOW("rrssa");

        private final String cliName;

        AvailableBackend(String cliName) {
            this.cliName = cliName;
        }

        @Override
        public String toString() {
            return cliName;
        }
    }

    /**
     * Instantiates a {@link Backend} according to the passed {@link AvailableBackend} enum value.
     * <p>
     * If the value <code>null</code> was passed, this method returns a <code>null</code> value.
     * </p>
     *
     * @throws ErrorMessage.InternalError If the passed {@link AvailableBackend} is not known by the compiler.
     */
    public static Backend instantiateBackend(AvailableBackend availableBackend) throws ErrorMessage.InternalError {
        if (availableBackend == null) {
            return null;
        }

        return switch (availableBackend) {
            case INTERPRETER -> new InterpreterBackend();
            case TAC_BACKEND -> new TacBackend();
            case RSSA_ORIGINAL -> new RSSABackend.OriginalRSSABackend();
            case RSSA_IMPROVED_CONTROL_FLOW -> new RSSABackend.ReversingControlFlowRSSABackend();
        };
    }

    /**
     * Returns a default {@link Backend} instance.
     */
    public static Backend getDefaultBackend() {
        return instantiateBackend(AvailableBackend.RSSA_IMPROVED_CONTROL_FLOW);
    }


    public final DumpOptions dumpOptions;
    public final boolean printMain;
    public final Backend selectedBackend;

    boolean diagnosticEnabled = false;
    private final OptimizationState optimizationState;

    private final File inputFile, outputFile;

    public final List<Pass> passes;

    public Compiler(DumpOptions dumpOptions, boolean printMain, Backend selectedBackend,
                    OptimizationState optimizationState,
                    File inputFile, File outputFile) {
        this.dumpOptions = dumpOptions;
        this.printMain = printMain;
        this.selectedBackend = selectedBackend;
        this.optimizationState = optimizationState;
        this.inputFile = inputFile;
        this.outputFile = outputFile;

        optimizationState.enableImpliedOptimizations();

        this.passes = List.of(
                new GlobalAnalysisPass(),
                new LocalAnalysisPass(),
                new AliasingAnalysisPass(),
                new LoopOptimizerPass(optimizationState)
        );
    }

    /**
     * Returns the {@link OptimizationState}, holding information about
     * which {@link AvailableOptimization optimizations} are enabled,
     * their arguments and debug state.
     */
    public OptimizationState getOptimizationState() {
        return optimizationState;
    }

    /**
     * Checks whether or not an optimization was enabled by the user. If the user did not
     * explicitly specify the given optimization, the return value of this method depends
     * on whether the {@link AvailableOptimization} is enabled on the current optimization
     * level.
     */
    public boolean isOptimizationEnabled(AvailableOptimization optimization) {
        return optimizationState.isOptimizationEnabled(optimization);
    }

    /**
     * Returns a {@link List} of parameters that were passed by the user while selecting an enabled optimization.
     * If no explicit parameters were passed, an empty list is returned.
     *
     * @throws IllegalStateException If the option does not accept any parameters.
     */
    public List<String> getOptimizationParameters(AvailableOptimization optimization) throws IllegalStateException {
        return optimizationState.getOptimizationParameters(optimization);
    }

    /**
     * Reads the input file and runs this {@link Compiler} on the read input. If a {@link Backend} is selected
     * and this {@link Backend} is executed, output of this {@link Backend} is written to the output file.
     * <p>
     * If the output file is <code>null</code> the output is written to {@link System#out} instead.
     * </p>
     *
     * @throws IOException  If an {@link IOException} occurs while reading input or opening the output file.
     * @throws ErrorMessage If an {@link ErrorMessage} occurs during any of the {@link Pass Passes} or the {@link Backend}.
     */
    public void compile() throws IOException, ErrorMessage {
        long startTime = System.nanoTime();
        try {
            doCompile();
        } finally {
            printPassedTime(startTime);
        }
    }

    private void printPassedTime(long startTime) {
        if (diagnosticEnabled) {
            double passedMicroseconds = (System.nanoTime() - startTime) /
                    (double) TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS);
            System.out.printf("Compilation finished after %.2f ms.\n",
                    Math.round(passedMicroseconds * 100.0) / 100.0); // Round to two digits.
        }
    }

    // Compile implementation.
    private void doCompile() throws IOException, ErrorMessage {
        try (Source source = Source.fromFileOrDefault(inputFile);
             Sink sink = Sink.fromFileOrDefault(outputFile)) {
            // Open required debug output.
            dumpOptions.initialize(inputFile);

            if (dumpOptions.dumpOptimizationState) {
                optimizationState.dumpState(System.out);
                return;
            }

            Scanner scanner = createScanner(Scanner::new, source);
            if (dumpOptions.dumpTokens) {
                // Show all tokens and exit.
                dumpTokens(scanner);
                return;
            }

            // Parse input.
            Parser parser = new Parser(scanner, new JanusSymbolFactory());
            Program program = (Program) parse(parser);

            if (dumpOptions.dumpAst) {
                // Show the result of parsing and exit.
                System.out.println(program);
            }

            Environment environment = new Environment();

            // Execute all passes in specified order
            for (Pass pass : passes) {
                pass.execute(this, environment, program);
            }

            if (selectedBackend == null) {
                return;
            }

            selectedBackend.executeBackend(this, environment, program, sink.getOutputStream());

        }
    }
}
