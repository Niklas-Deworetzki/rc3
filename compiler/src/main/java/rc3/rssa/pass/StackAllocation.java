package rc3.rssa.pass;

import rc3.lib.utils.CollectionUtils;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;
import rc3.rssa.instances.Variable;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides static methods to allocate a stack frame for an RSSA procedure.
 * <p>
 * The {@link StackAllocation#allocateStackFrames(Program)} method allocates a stack frame
 * for every {@link ControlGraph procedure} of a {@link Program}. Additionally, a
 * {@link StackAllocation.Strategy strategy used for allocation} can be provided.
 * <p>
 * If no strategy is provided, the default strategy is used. It will allocate a stack slot
 * for every {@link rc3.rssa.instances.Variable} and set the count of simultaneously required
 * slots as the stack frame size.
 */
public final class StackAllocation {
    public static final int NOT_ALLOCATED = -111;

    /**
     * Creates the default {@link StackAllocation.Strategy} for a given {@link ControlGraph}.
     * <p>
     * The strategy returned from this method is not specialized for any particular use case
     * or backend. Therefore, it may not always produce the most optimal layout.
     * <p>
     * It also <b>does not</b> include any special registers, that might appear in a stack frame
     * that have to be saved during a context change.
     */
    public static StackAllocation.Strategy defaultStrategy(ControlGraph procedure) {
        return new LinearScan(procedure);
    }

    /**
     * Allocates a stack frame layout for every procedure in the given {@link Program}
     * using the {@link StackAllocation#defaultStrategy(ControlGraph) default strategy}.
     */
    public static void allocateStackFrames(Program program) {
        allocateStackFrames(program, StackAllocation::defaultStrategy);
    }

    /**
     * Allocates a stack frame layout for every procedure in the given {@link Program}.
     * For every {@link ControlGraph procedure} individually the given constructor is
     * invoked.
     */
    public static void allocateStackFrames(Program program,
                                           Function<ControlGraph, StackAllocation.Strategy> strategyConstructor) {
        for (ControlGraph procedure : program.procedures().values()) {
            allocateStackFrame(procedure, strategyConstructor);
        }
    }


    /**
     * Allocates a stack frame layout for the given procedure using the
     * {@link StackAllocation#defaultStrategy(ControlGraph) default strategy}.
     */
    public static void allocateStackFrame(ControlGraph procedure) {
        allocateStackFrame(procedure, StackAllocation::defaultStrategy);
    }

    /**
     * Allocates a stack frame layout for the given procedure using the
     * {@link StackAllocation.Strategy} created from the given constructor.
     */
    public static void allocateStackFrame(ControlGraph procedure,
                                          Function<ControlGraph, StackAllocation.Strategy> strategyConstructor) {
        final StackAllocation.Strategy strategy = strategyConstructor.apply(procedure);

        procedure.frameSize = strategy.requiredStackSpace();
        forEveryVariable(procedure, variable ->
                variable.offset = strategy.offsetFor(variable));
    }

    // Apply a Consumer to every Variable in a procedure.
    private static void forEveryVariable(ControlGraph procedure, Consumer<? super Variable> action) {
        for (BasicBlock block : procedure) {
            for (Instruction instruction : block) {
                for (Variable variable : instruction.variablesCreated()) {
                    action.accept(variable);
                }
                for (Variable variable : instruction.variablesUsed()) {
                    action.accept(variable);
                }
                for (Variable variable : instruction.variablesDestroyed()) {
                    action.accept(variable);
                }
            }
        }
    }

    /**
     * Print the stack layout for every procedure in the given {@link Program}.
     *
     * @param program A {@link Program} containing procedures with a stack layout.
     * @param stream  A {@link PrintStream} to which the output is written to.
     * @throws IllegalStateException If any procedure in the given {@link Program}
     *                               has no stack layout allocated.
     */
    public static void dumpStackLayout(Program program, PrintStream stream) throws IllegalStateException {
        for (ControlGraph procedure : program.procedures().values()) {
            dumpStackLayout(procedure, stream);
        }
    }

    /**
     * Print the stack layout for the given procedure.
     *
     * @param procedure The procedure for which the stack layout should be printed.
     * @param stream    A {@link PrintStream} to which the output is written to.
     * @throws IllegalStateException If the given procedure has no stack layout allocated.
     */
    public static void dumpStackLayout(ControlGraph procedure, PrintStream stream) throws IllegalStateException {
        if (procedure.frameSize == NOT_ALLOCATED) throw new IllegalStateException("No stack frame is allocated!");

        List<Set<Variable>> slots = CollectionUtils.fillList(HashSet::new, procedure.frameSize);
        forEveryVariable(procedure, variable ->
                slots.get(variable.offset).add(variable));


        stream.println("Stack layout for procedure " + procedure.getProcedureName());
        for (int i = 0; i < slots.size(); i++) {
            stream.printf("%3d: %s%n", i, slots.get(i).stream()
                    .map(Variable::getName)
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
    }

    /**
     * An interface to the allocation strategy. Instances implementing this interface
     * can be used to allocate stack space for procedures and the offsets for variables.
     */
    public interface Strategy {
        int requiredStackSpace();

        int offsetFor(Variable variable);
    }
}
