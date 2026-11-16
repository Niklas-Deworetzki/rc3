package rc3.rssa.control;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.lib.DirectionVar;
import rc3.lib.messages.WarningMessage;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.utils.SetOperations;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.HashSet;
import java.util.Set;

import static rc3.lib.utils.CollectionUtils.anyElement;
import static rc3.lib.utils.SetOperations.intersects;

/**
 * {@link rc3.rssa.instances.Program.ProcedureOptimization} used to eliminate unreachable
 * parts of an RSSA {@link ControlGraph}. Entirely unreachable {@link BasicBlock}s are
 * removed from the {@link ControlGraph} and {@link rc3.rssa.instances.ControlInstruction.Conditional}
 * instructions associated with one of the unreachable labels are converted into
 * {@link rc3.rssa.instances.ControlInstruction.Unconditional} variants.
 * This creates opportunities for further optimizations.
 * <p>
 * The safe variant of this optimization only removes labels that are unreachable in both
 * execution directions. The unsafe variant tries to remove labels that are unreachable in
 * only one of both directions. If a label is unreachable in one direction, jumping to it
 * in the other direction will <b>always</b> be an irreversible operation and therefore
 * result in a runtime error or undefined behavior. Eliminating these paths therefore might
 * change the program's behavior, as error conditions are potentially unchecked or runtime
 * errors mitigated. However, under the assumption that only correctly reversible programs
 * are provided as an input to this optimization, one could argue that the unsafe variant
 * only removes labels unreachable in both directions. If the label was reached, an
 * incorrect program would be present, invalidating the assumption. Another point of view
 * is that the compiler/optimizer is merely not smart enough to detect, that a condition
 * will always evaluate to a certain boolean value and therefore make a label unreachable
 * in both execution directions.
 * <p>
 * The analysis which labels are reachable is done by {@link ReachableControlGraph}, which
 * follows paths through a {@link ControlGraph} starting at (depending on the analysis direction)
 * {@link rc3.rssa.instances.BeginInstruction} or {@link rc3.rssa.instances.EndInstruction}.
 * Edges from {@link rc3.rssa.instances.ControlInstruction.Conditional} control points are
 * only used for this analysis, if {@link BinaryOperand#compareConstant()} has an according
 * statically known value or is runtime-dependent.
 * <p>
 * This optimization creates opportunities for analysis of a routine's control flow.
 * <ul>
 *  <li>If the labels reachable in forward direction and the labels reachable in
 *  backward direction do not intersect, there does not exist a path through a
 *  routine, connecting the begin and end.</li>
 *  <li>If a {@link BasicBlock} only contains unreachable labels at its entry or exit
 *  point, but contains reachable labels at the other point, a conflict is present in
 *  the control flow of a routine. Executing that {@link BasicBlock} will always cause
 *  a runtime error and the aggressive optimisation variant is not possible in those
 *  cases. Performing the aggressive optimisation would create a {@link BasicBlock}
 *  without entry or exit point.</li>
 * </ul>
 */
public final class EliminateUnreachableSubgraphs extends Program.ProcedureOptimization {
    public EliminateUnreachableSubgraphs(Compiler compiler) {
        super(AvailableOptimization.ELIMINATE_UNREACHABLE, compiler);
    }

    private boolean allowUnsafe() {
        return optimizationState.isOptimizationEnabled(AvailableOptimization.ELIMINATE_UNREACHABLE_UNSAFE);
    }

    @Override
    public ControlGraph apply(String s, ControlGraph graph) {
        final var reachableGraph = new ReachableControlGraph(graph);
        debug(reachableGraph::toString);

        // Compute reachable labels.
        final DirectionVar<Set<String>> reachableLabels =
                DirectionVar.instantiate(reachableGraph::reachableLabels);
        final boolean isDisconnected = isDisconnected(reachableLabels);
        if (isDisconnected) {
            new WarningMessage.DisconnectedRoutine(graph).report();
        }

        // Negate reachable Sets in both directions to get unreachable labels.
        final DirectionVar<Set<String>> unreachableLabels =
                DirectionVar.of(() -> new HashSet<>(graph.getLabels()));
        unreachableLabels.forEach((direction, labels) ->
                labels.removeAll(reachableLabels.get(direction)));

        final Set<String> unreachableUnion = unreachableLabels.fold(SetOperations::union);
        final boolean containsConflict = containsConflictingConditions(graph, unreachableUnion);
        if (containsConflict && !isDisconnected) { // Only create a warning once.
            new WarningMessage.ControlFlowConflict(graph).report();
        }

        final Set<String> labelsToRemove;
        if (allowUnsafe() && !containsConflict) {
            labelsToRemove = unreachableUnion;
        } else {
            labelsToRemove = unreachableLabels.fold(SetOperations::intersection);
        }

        return new Optimizer(graph, labelsToRemove).eliminateUnreachable();
    }

    /**
     * If the {@link Set} of forwards reachable labels and the {@link Set} of
     * backwards reachable labels are disjoint, no path through the routine exists.
     * If both {@link Set}s are empty, there simply is no label and a routine
     * must always be connected.
     */
    private boolean isDisconnected(DirectionVar<Set<String>> reachableLabels) {
        return !reachableLabels.fold(SetOperations::intersects) &&
                !reachableLabels.map(Set::isEmpty).fold(Boolean::logicalAnd);
    }

    /**
     * A conflict with the static conditions of a program exists, if a {@link BasicBlock} is
     * reachable in one direction but not the other. This would require some sort of branching
     * within the {@link BasicBlock} to redirect control flow. This, by definition, is not
     * possible as {@link BasicBlock}s are always executed sequentially.
     * <p>
     * A conflict can only occur in {@link BasicBlock}s with
     * {@link rc3.rssa.instances.ControlInstruction.Conditional} entry or exit points.
     * Every conditional creates two paths and a conflict occurs if both paths are
     * somehow unreachable. This is only possible, if one path is unreachable in forwards
     * direction and the other path is unreachable in backwards direction.
     * As a result, a conflict will always cause a runtime error if one of both paths is
     * executed.
     */
    private static boolean containsConflictingConditions(ControlGraph graph, Set<String> unreachable) {
        for (BasicBlock block : graph) {
            final boolean fwUnreachable = isUnreachable(unreachable, block, Direction.FORWARD);
            final boolean bwUnreachable = isUnreachable(unreachable, block, Direction.BACKWARD);

            if (fwUnreachable != bwUnreachable) {
                return true; // Conflict: Block is unreachable from one direction but not the other.
            }
        }
        return false;
    }


    /**
     * A class used to optimize the control flow of a given routine by removing a
     * {@link Set} of unreachable labels from it. Labels are removed by updating
     * {@link ControlInstruction}s they appear in and removing entirely unreachable
     * {@link BasicBlock}s from the {@link ControlGraph}.
     *
     * @param graph             The {@link ControlGraph} of a routine to optimize.
     * @param unreachableLabels The {@link Set} of labels that are marked unreachable
     *                          and should be removed from the {@link ControlGraph}.
     */
    private record Optimizer(ControlGraph graph, Set<String> unreachableLabels) {

        public ControlGraph eliminateUnreachable() {
            final Set<BasicBlock> blocksForRemoval = removeLabels();
            graph.getNodes().removeAll(blocksForRemoval);
            return graph;
        }

        /**
         * Remove labels by {@link Optimizer#updateControlPoint(Direction, BasicBlock) updating
         * the entry and exit points of blocks}. Entirely unreachable {@link BasicBlock}s are
         * detected and returned by this method.
         */
        private Set<BasicBlock> removeLabels() {
            final Set<BasicBlock> blocksForRemoval = new HashSet<>();
            for (BasicBlock block : graph) {
                final boolean fwUnreachable = isUnreachable(unreachableLabels, block, Direction.FORWARD);
                final boolean bwUnreachable = isUnreachable(unreachableLabels, block, Direction.BACKWARD);

                if (fwUnreachable && bwUnreachable) { // Block is entirely unreachable. Mark it for removal.
                    blocksForRemoval.add(block);
                } else {
                    // Block is reachable but might be associated with unreachable labels.
                    if (intersects(unreachableLabels, block.entryLabels()))
                        updateControlPoint(Direction.FORWARD, block);
                    if (intersects(unreachableLabels, block.exitLabels()))
                        updateControlPoint(Direction.BACKWARD, block);
                }
            }
            return blocksForRemoval;
        }

        private void updateControlPoint(Direction direction, BasicBlock block) {
            final int index = direction.choose(0, block.getInstructions().size() - 1);
            block.getInstructions().set(index,
                    updateInstruction((ControlInstruction) block.getInstructions().get(index)));
        }

        private ControlInstruction updateInstruction(ControlInstruction instruction) {
            final Set<String> associatedLabels = new HashSet<>(instruction.associatedLabels());
            associatedLabels.removeAll(unreachableLabels);

            // Get the label that is not marked as unreachable.
            assert associatedLabels.size() == 1;
            final String remainingLabel = anyElement(associatedLabels);

            final ControlInstruction result = instruction.isEntryPoint()
                    ? new UnconditionalEntry(remainingLabel)
                    : new UnconditionalExit(remainingLabel);
            result.setParameters(instruction.getParameters());
            return result;
        }
    }

    /**
     * Returns <code>true</code>, if a {@link BasicBlock} is unreachable in a given execution
     * {@link Direction}.
     * <p>
     * It is unreachable, if all labels through which it can be entered in an execution
     * direction are marked as unreachable. The {@link rc3.rssa.instances.BeginInstruction}
     * and {@link rc3.rssa.instances.EndInstruction} do not provide labels through which
     * a {@link BasicBlock} can be entered, but are always considered reachable.
     */
    private static boolean isUnreachable(Set<String> unreachableLabels, BasicBlock block,
                                         Direction direction) {
        final Set<String> labels = direction.chooseLazy(block::entryLabels, block::exitLabels);
        return !labels.isEmpty() && unreachableLabels.containsAll(labels);
    }
}
