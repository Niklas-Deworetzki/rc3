package rc3.januscompiler.backend.rssa;

import rc3.lib.DumpOptions;
import rc3.rssa.UnusedProcedureElimination;
import rc3.rssa.control.ControlGraphSimplification;
import rc3.rssa.control.EliminateUnreachableSubgraphs;
import rc3.rssa.cp.GlobalConstantPropagation;
import rc3.rssa.cp.LocalConstantPropagation;
import rc3.rssa.dce.DeadCodeElimination;
import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.januscompiler.backend.AnnotationMode;
import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.backend.rssa.toC.CTranslator;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.table.Environment;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.cse.CommonSubexpressionElimination;
import rc3.rssa.inlining.Inlining;
import rc3.rssa.translation.MemoryManagement;
import rc3.rssa.translation.RSSAGenerator;
import rc3.rssa.translation.StatementRSSAGenerator;
import rc3.rssa.used.LiveVariables;
import rc3.rssa.used.UsedVariables;
import rc3.rssa.vm.RSSAVM;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

import static picocli.CommandLine.Option;

public abstract class RSSABackend extends Backend {

    public static final int DEFAULT_HEAP_SIZE = 102400;
    public static final int DEFAULT_STACK_SIZE = 256;


    // Public members are instantiated via picocli by user input.

    @Option(names = "heap-size",
            description = "Amount of total heap memory allocated at runtime for the generated program.%n" +
                    "Default: " + DEFAULT_HEAP_SIZE)
    public int heapSize = DEFAULT_HEAP_SIZE;

    @Option(names = "stack-size",
            description = "Amount of memory allocated for every created stack.%n" +
                    "Default: " + DEFAULT_STACK_SIZE)
    public int stackSize = DEFAULT_STACK_SIZE;

    @Option(names = "stop-runtime",
            description = "Adds profiling instructions to the generated C code to measure the runtime of compiled programs.%n" +
                    "Runtime is measured in clocks.")
    public boolean measureRuntime = false;

    @Option(names = "count-instructions",
            description = "Adds profiling instructions to the generated C code to measure the amount of executed instructions.")
    public boolean countExecutedInstructions = false;

    @Option(names = "vm",
            description = "Runs the generated RSSA code with an RSSA VM.")
    public boolean vm = false;

    // TODO: Does it make sense to start compiled janus programs backwards?
    @Option(names = {"backward", "bw"},
            description = "The VM starts the execution of the RSSA code backwards, if the vm is selected.")
    public boolean backwardExecution = false;

    /**
     * This method is used by the concrete implementations of this {@link Backend} to determine how code for
     * statements is translated into RSSA Instructions.
     */
    protected abstract StatementRSSAGenerator constructStatementGenerator(RSSAGenerator generator);

    @Override
    protected void execute(Compiler compiler, Environment environment, Program program, PrintStream out) {
        final MemoryManagement memoryManagement = new MemoryManagement(stackSize, heapSize);

        var rssaProgram = RSSAGenerator.generateProgramCode(this::constructStatementGenerator, program, memoryManagement, environment);

        // TODO: Optimization order

        if (compiler.isOptimizationEnabled(AvailableOptimization.INLINING)) {
            rssaProgram = new Inlining(compiler, rssaProgram).performOptimization();
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.SIMPLIFY_CONTROL_FLOW)) {
            rssaProgram.applyOptimization(new ControlGraphSimplification(compiler));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.COMMON_SUBEXPRESSION_ELIMINATION)) {
            rssaProgram.applyOptimization(new CommonSubexpressionElimination(compiler, rssaProgram));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.GLOBAL_CONSTANT_PROPAGATION)) {
            rssaProgram.applyOptimization(new GlobalConstantPropagation(compiler));
        } else if (compiler.isOptimizationEnabled(AvailableOptimization.LOCAL_CONSTANT_PROPAGATION)) {
            rssaProgram.applyOptimization(new LocalConstantPropagation(compiler));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.ELIMINATE_UNREACHABLE)) {
            rssaProgram.applyOptimization(new EliminateUnreachableSubgraphs(compiler));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.SIMPLIFY_CONTROL_FLOW)) {
            rssaProgram.applyOptimization(new ControlGraphSimplification(compiler));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.LIVE_VARIABLES)) {
            LiveVariables.dumpLiveVariables(rssaProgram);
        }
        if (compiler.isOptimizationEnabled(AvailableOptimization.USED_VARIABLES)) {
            UsedVariables.dumpLiveVariables(rssaProgram);
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.DEAD_CODE_ELIMINATION)) {
            rssaProgram.applyOptimization(new DeadCodeElimination(compiler));
        }

        if (compiler.isOptimizationEnabled(AvailableOptimization.UNUSED_PROCEDURES_ELIMINATION)) {
            new UnusedProcedureElimination(compiler, rssaProgram).performOptimization();
        }

        if (compiler.dumpOptions.dumpIntermediate) {
            dumpIntermediateCode(compiler.dumpOptions, rssaProgram);

        } else {
            if (vm) {
                // start RSSA vm here if wanted
                var initialDirection = backwardExecution ?
                        Direction.BACKWARD : Direction.FORWARD;

                final RSSAVM rssaVm = new RSSAVM(rssaProgram, initialDirection,
                        out, compiler.printMain, countExecutedInstructions);
                rssaVm.run();
            } else {
                new CTranslator(AnnotationMode.SOURCE_ANNOTATIONS, measureRuntime, countExecutedInstructions)
                        .emitCCode(compiler, out, rssaProgram);
            }
        }
    }

    private static void dumpIntermediateCode(DumpOptions dumpOptions,
                                             rc3.rssa.instances.Program rssaProgram) {

        if (!dumpOptions.graphDumpRequested) {
            rssaProgram.globalAnnotations().forEach(System.out::println);
        }

        for (Map.Entry<String, ControlGraph> graphEntry : rssaProgram.procedures().entrySet()) {
            if (dumpOptions.graphDumpRequested) {
                try (PrintStream out = dumpOptions.getGraphDumpStream(graphEntry.getKey())) {
                    out.println(graphEntry.getValue().graphString());
                } catch (IOException ioException) {
                    System.err.println("Cannot generate graph output: " + ioException.getMessage());
                }

            } else {
                System.out.println("// Code for procedure " + graphEntry.getKey() + ":");
                graphEntry.getValue().toCode().forEach(System.out::println);
                System.out.println();
            }
        }
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("This backend is used to translate Janus into a reversible intermediate code. " +
                "The so-called RSSA code is based on T. Mogensen, RSSA: A Reversible SSA Form, S. 213. " +
                "In: Perspectives of System Informatics 2015.");
    }

    // Original RSSA backend.
    public static final class OriginalRSSABackend extends RSSABackend {
        @Override
        protected StatementRSSAGenerator constructStatementGenerator(RSSAGenerator generator) {
            return new StatementRSSAGenerator.AssigningConditionalGenerator(generator);
        }

        @Override
        public String getDisplayName() {
            return "RSSA Backend (legacy backend, uses temporary variables in conditions)";
        }
    }

    // New RSSA backend using reversible code generation in control flows.
    public static final class ReversingControlFlowRSSABackend extends RSSABackend {
        @Override
        protected StatementRSSAGenerator constructStatementGenerator(RSSAGenerator generator) {
            return new StatementRSSAGenerator.ReversingConditionalGenerator(generator);
        }

        @Override
        public String getDisplayName() {
            return "RSSA Backend (uses reversed code generation for conditions)";
        }
    }
}
