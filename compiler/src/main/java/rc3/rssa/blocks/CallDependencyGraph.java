package rc3.rssa.blocks;

import rc3.lib.utils.CollectionUtils;
import rc3.rssa.instances.CallInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;

import java.util.*;

/**
 * This class provides utility methods to analyze the structure of a {@link Program} by traversing
 * procedure calls. It allows to inspect the dependencies between procedures to enable elimination
 * of unused procedures or propagation of procedure-level properties such like "pureness".
 */
public final class CallDependencyGraph {
    private final Map<String, List<String>> calledProcedures, callingProcedures;

    private final Program program;

    public static CallDependencyGraph constructGraph(Program program) {
        return new CallDependencyGraph(program);
    }

    private CallDependencyGraph(Program program) {
        this.program = program;

        // Prepare data stores for this instance, before initializing them with actual data.
        this.calledProcedures = new HashMap<>(program.procedureNames().size());
        this.callingProcedures = new HashMap<>(program.procedureNames().size());
        for (String procedureName : program.procedureNames()) {
            calledProcedures.put(procedureName, new ArrayList<>());
            callingProcedures.put(procedureName, new ArrayList<>());
        }
        initialize();
    }

    private void initialize() {
        for (String procedureName : program.procedureNames()) {
            ControlGraph procedureCode = program.getProcedure(procedureName);

            for (String calledProcedure : extractCalledProcedures(procedureCode.toCode())) {
                calledProcedures.get(procedureName).add(calledProcedure);
                callingProcedures.get(calledProcedure).add(procedureName);
            }
        }
    }

    private Collection<String> extractCalledProcedures(List<Instruction> instructions) {
        Set<String> calledProcedures = new HashSet<>();

        for (Instruction instruction : instructions) {
            if (instruction instanceof CallInstruction) {
                calledProcedures.add(((CallInstruction) instruction).procedure);
            }
        }

        return calledProcedures;
    }

    /**
     * Returns a {@link Set} holding the names of all procedures present in the program.
     */
    public Set<String> allProcedures() {
        return calledProcedures.keySet();
    }

    /**
     * Returns a {@link Collection} of procedures that are <b>directly</b> called by the <var>calling procedure</var>.
     */
    public Collection<String> getCalledProcedures(String callingProcedure) {
        return calledProcedures.get(callingProcedure);
    }

    /**
     * Returns a {@link Collection} of procedures that are either directly or indirectly called by the
     * <var>calling procedure</var>. An indirect call occurs if a called procedure itself calls
     * another procedure (with arbitrary depth of nested calls).
     */
    public Collection<String> getAllCalledProcedures(String callingProcedure) {
        return CollectionUtils.traverseGraph(ArrayList::new, this::getCalledProcedures, false, callingProcedure);
    }

    /**
     * Returns a {@link Collection} of procedures that <b>directly</b> call this <var>called procedure</var>.
     */
    public Collection<String> getCallingProcedures(String calledProcedure) {
        return callingProcedures.get(calledProcedure);
    }

    /**
     * Returns a {@link Collection} of procedures that either directly or indirectly call this
     * <var>called procedure</var>. An indirect call occurs if the procedure calling this
     * procedure is itself called by yet another procedure (with arbitrary depth of nested calls).
     */
    public Collection<String> getAllCallingProcedures(String calledProcedure) {
        return CollectionUtils.traverseGraph(ArrayList::new, this::getCallingProcedures, false, calledProcedure);
    }

    /**
     * Returns whether or not the given procedure is called by any other procedure.
     */
    public boolean isProcedureCalled(String procedure) {
        return callingProcedures.get(procedure).isEmpty();
    }

    /**
     * Returns whether or not the given procedure calls another procedure. If it does not call any other
     * procedure, it is a leaf procedure.
     */
    public boolean isLeafProcedure(String procedure) {
        return calledProcedures.get(procedure).isEmpty();
    }

    /**
     * Returns whether or not the given procedure is recursive. A recursive procedure is a procedure, that
     * either directly or indirectly calls itself. This method does not differentiate between direct or
     * indirect recursion.
     *
     * @see CallDependencyGraph#isDirectlyRecursive(String)
     * @see CallDependencyGraph#isIndirectlyRecursive(String)
     */
    public boolean isRecursive(String procedure) {
        return isDirectlyRecursive(procedure) || isIndirectlyRecursive(procedure);
    }

    /**
     * Returns whether or not the given procedure is directly recursive. A directly recursive procedure
     * contains a direct call to itself in its body without calling any other procedures.
     */
    public boolean isDirectlyRecursive(String procedure) {
        return getCalledProcedures(procedure).contains(procedure);
    }

    /**
     * Returns whether or not the given procedure is indirectly recursive. An indirectly recursive procedure
     * contains calls in its body which eventually call the procedure again. This method only considers
     * a procedure as indirectly recursive <b>iff</b> it is recursive but no direct recursive calls are made.
     */
    public boolean isIndirectlyRecursive(String procedure) {
        return !isDirectlyRecursive(procedure) && getAllCalledProcedures(procedure).contains(procedure);
    }
}
