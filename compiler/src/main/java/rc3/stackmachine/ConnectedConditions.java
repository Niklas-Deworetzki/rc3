package rc3.stackmachine;

import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.*;

import static rc3.lib.utils.CollectionUtils.anyElement;
import static rc3.lib.utils.SetOperations.concat;
import static rc3.rssa.blocks.ConnectedControlPoints.connectedGroups;

/**
 * This utility class provides a single public facing method:
 * {@link ConnectedConditions#parametersRequiringMove(ControlGraph)}
 * <p>
 * The purpose of this class is to precompute which parameters in a parameter
 * {@link List} of a {@link BasicBlock block's} entry or exit point require a
 * move, as they change their stack frame position between the different blocks.
 * Additionally, it provides an order in which parameters can be placed on the
 * stack. This order tries to optimize the positioning of parameters on the stack
 * in a way, that parameters required in the evaluation of a condition are placed
 * later, since they interfere with the condition and stack shuffling instructions
 * must be generated.
 * <p>
 * Between two {@link Atom Atoms (parameters)} this can be determined rather
 * simple by using {@link Translation#requiresMove(Atom, Atom)}, which specifies
 * the different circumstances in which a move can be omitted.
 * In a complex {@link ControlGraph} determining this property becomes harder.
 *
 * <pre>
 *  begin A
 *  B ->
 *
 *  B()C <-
 *  C()D ->
 *
 *  D <-
 *  end A
 * </pre>
 * <p>
 * Considering this example, not only do the parameters of B have to match. Since
 * the second block has a conditional entry point that combines B and C, both it is
 * required that C has the same properties for its parameters to omit a move. However,
 * C appears in a conditional control point as well, which pulls D into this equation.
 * <p>
 * This class detects these groups of connected labels and then analyses the parameters
 * in all of a group's parameter lists. The results of this analysis are stored as a
 * <code>boolean</code> value per parameter, indicating whether this parameter requires
 * a move. Additionally, this information is present for each label instead for each group,
 * so that a caller does not have to identify different connected groups.
 */
final class ConnectedConditions {

    public static Map<String, int[]> parametersRequiringMove(ControlGraph graph) {
        final Map<String, int[]> result = new HashMap<>();
        for (Set<ControlInstruction> connectedGroup : connectedGroups(graph)) {
            final var parameterInfo = parameterOrder(connectedGroup);

            // Add information about parameters for every label in group.
            for (ControlInstruction instruction : connectedGroup) {
                for (String label : instruction.associatedLabels()) {
                    result.put(label, parameterInfo);
                }
            }
        }
        return result;
    }

    /**
     *
     */
    private record ParameterPosition(int index, int score) implements Comparable<ParameterPosition> {
        @Override
        public int compareTo(ParameterPosition other) {
            return Integer.compare(this.score, other.score);
        }
    }

    /**
     * Returns an array of indices. Each index refers to a parameter, that must be moved
     * in the given group. The order of indices tries to minimize the need of stack shuffling
     * instructions generated.
     * <p>
     * All indices contained in the returned array are from parameters, that require a move.
     * If it is not required to move a parameter, its index won't appear as part of the result.
     * Indices are ordered by their amount of occurrences in conditions. This way, the overall
     * least amount of stack shuffling instructions is generated, minimizing code size. No
     * assumptions about execution frequency are made.
     */
    private static int[] parameterOrder(Set<ControlInstruction> connectedGroup) {
        final int parameterCount = anyElement(connectedGroup).getParameters().size();

        final List<Boolean> requiresMove = parametersRequiringMoveInGroup(connectedGroup);
        final List<ParameterPosition> parameterPositions = new ArrayList<>(parameterCount);
        for (int index = 0; index < parameterCount; index++) {
            if (!requiresMove.get(index)) continue; // Skip parameters that don't require move.

            int occurrences = 0;
            // Count how often, parameters in this position occur in a condition.
            for (ControlInstruction controlInstruction : connectedGroup) {
                final Atom parameter = controlInstruction.getParameters().get(index);
                if (variablesInCondition(controlInstruction).contains(parameter)) {
                    occurrences++;
                }
            }
            parameterPositions.add(new ParameterPosition(index, occurrences));
        }

        Collections.sort(parameterPositions);

        int[] result = new int[parameterPositions.size()];
        for (int i = 0; i < parameterPositions.size(); i++) {
            result[i] = parameterPositions.get(i).index;
        }
        return result;
    }

    /**
     * Returns the {@link Set} of {@link Variable} instances that are used within
     * the condition of the given {@link ControlInstruction}. If the {@link ControlInstruction}
     * is neither a {@link ConditionalEntry} or {@link ConditionalExit}, an empty {@link Set}
     * is returned.
     */
    private static Set<Variable> variablesInCondition(ControlInstruction instruction) {
        if (instruction instanceof ConditionalEntry entry)
            return new HashSet<>(concat(entry.left.variablesUsed(), entry.right.variablesUsed()));
        if (instruction instanceof ConditionalExit exit)
            return new HashSet<>(concat(exit.left.variablesUsed(), exit.right.variablesUsed()));
        return Collections.emptySet();
    }

    /**
     * Creates a {@link List} of <code>boolean</code> values, each representing whether parameters
     * at the given index should be moved. This is done by constructing a {@link List} of {@link Atom}
     * that hold the <var>i</var>-th {@link Atom} of every {@link ControlInstruction}'s parameter list
     * within the given group. This {@link List} is then passed to the method
     * {@link ConnectedConditions#atomsRequireMove(List)} to get the associated <code>boolean</code> value.
     */
    private static List<Boolean> parametersRequiringMoveInGroup(Set<ControlInstruction> connectedGroup) {
        final Boolean[] result = new Boolean[anyElement(connectedGroup).getParameters().size()];
        assert connectedGroup.stream() // All parameter lists should have equal length.
                .map(ControlInstruction::getParameters)
                .mapToInt(List::size)
                .allMatch(size -> size == result.length) : "Connected group of ControlInstructions has different length in parameter list";

        final List<Atom> parameters = new ArrayList<>(connectedGroup.size());
        for (int index = 0; index < result.length; index++) {
            // Sample relevant parameters.
            for (ControlInstruction instruction : connectedGroup) {
                parameters.add(instruction.getParameters().get(index));
            }

            result[index] = atomsRequireMove(parameters);
            parameters.clear(); // Re-use the same list as buffer.
        }

        return Arrays.asList(result);
    }

    /**
     * Determines whether a move is necessary for this parameter.
     * <p>
     * The {@link List} of {@link Atom Atoms} provided inhabit the same position
     * in different connected parameter lists. If a move is necessary between any
     * of them, this function returns <code>true</code>. Otherwise, it returns
     * <code>false</code>.
     * <p>
     * To decide, whether a pair of {@link Atom Atoms} requires a move, the
     * {@link Translation#requiresMove(Atom, Atom)} method is used.
     * This implementation assumes, that the property of not requiring a move
     * is transitive.
     */
    private static boolean atomsRequireMove(List<Atom> atoms) {
        for (int i = atoms.size() - 2; i >= 0; i--) {
            if (Translation.requiresMove(atoms.get(i), atoms.get(i + 1))) {
                return true;
            }
        }
        return false;
    }
}
