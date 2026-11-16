package rc3.rssa.blocks;

import rc3.rssa.instances.ControlInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;
import rc3.rssa.instances.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class VariableIndexer implements Consumer<Instruction> {

    public static void indexVariables(Program program) {
        for (ControlGraph controlGraph : program.procedures().values()) {
            indexVariables(controlGraph);
        }
    }

    public static void indexVariables(ControlGraph graph) {
        // The instance is shared between all basic blocks,
        // so variables are incremented continuously within a procedure.
        final var indexer = new VariableIndexer();
        for (BasicBlock node : graph) {
            node.getInstructions().forEach(indexer);
        }
    }

    private final Map<String, Integer> variableVersions = new HashMap<>();

    /**
     * Returns the current version of a {@link Variable} as stored in {@link VariableIndexer#variableVersions}.
     * If no version number is present, 0 is returned and inserted into {@link VariableIndexer#variableVersions}.
     */
    private int getCurrentVersion(Variable variable) {
        return variableVersions.computeIfAbsent(variable.getRawName(), ignored -> 0);
    }

    // Utility method used to initialize the variable number or increment it.
    private static int incrementVariableVersion(String ignoredVariableName, Integer currentVersion) {
        if (currentVersion == null) { // No version number was present, start with 0.
            return 0;
        } else {
            return currentVersion + 1;
        }
    }

    private void incrementAndIndex(Variable variable) {
        if (variable.number == Variable.UNINITIALIZED) {
            variableVersions.compute(variable.getRawName(), VariableIndexer::incrementVariableVersion);
            indexVariable(variable);
        } else {
            variableVersions.put(variable.getRawName(), variable.number + 1);
        }
    }

    private void indexVariable(Variable variable) {
        if (variable.number == Variable.UNINITIALIZED) {
            variable.number = getCurrentVersion(variable);
        }
    }

    @Override
    public void accept(Instruction instruction) {
        if (instruction instanceof ControlInstruction &&
                ((ControlInstruction) instruction).isEntryPoint()) {
            // Entry points produce new versions of variables, before they are used.

            instruction.variablesCreated().forEach(this::incrementAndIndex);
            instruction.variablesUsedOrDestroyed().forEach(this::indexVariable);
        } else {
            instruction.variablesUsedOrDestroyed().forEach(this::indexVariable);
            instruction.variablesCreated().forEach(this::incrementAndIndex);
        }
    }
}
