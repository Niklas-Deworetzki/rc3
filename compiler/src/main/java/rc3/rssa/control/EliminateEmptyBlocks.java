package rc3.rssa.control;

import rc3.januscompiler.Direction;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.ConditionalEntry;
import rc3.rssa.instances.UnconditionalEntry;
import rc3.rssa.instances.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static rc3.lib.utils.CollectionUtils.anyElement;
import static rc3.lib.utils.SetOperations.intersects;

/**
 * If an empty block starts with an unconditional entry and ends with an unconditional
 * exit, it only serves as <i>glue</i> to merge two other labels. Removing the
 * empty block and connecting the surrounding labels simplifies the structure of
 * a {@link ControlGraph}.
 */
public class EliminateEmptyBlocks implements ControlGraphSimplification.SimplificationAlgorithm {

    @Override
    public boolean isApplicable(ControlGraph graph, BasicBlock block) {
        return containsNoInstructions(block) &&
                controlPointsAreNotConditional(block) &&
                parameterListsContainOnlyVariables(block) &&
                parameterListsAreTheSame(block) &&
                isNotConnectedToItself(block);
    }

    private boolean containsNoInstructions(BasicBlock block) {
        return block.getInstructions().size() == 2;
    }

    private boolean controlPointsAreNotConditional(BasicBlock block) {
        return block.entryPoint().isUnconditional() &&
                block.exitPoint().isUnconditional();
    }

    private boolean parameterListsContainOnlyVariables(BasicBlock block) {
        return block.entryPoint().getParameters().stream().allMatch(Variable.class::isInstance) &&
                block.exitPoint().getParameters().stream().allMatch(Variable.class::isInstance);
    }

    private boolean parameterListsAreTheSame(BasicBlock block) {
        return Objects.equals(block.entryPoint().getParameters(), block.exitPoint().getParameters());
    }

    private boolean isNotConnectedToItself(BasicBlock block) {
        return !intersects(block.entryLabels(), block.exitLabels());
    }

    @Override
    public Set<BasicBlock> apply(ControlGraph graph, BasicBlock emptyBlock) {
        final String predecessorLabel = anyElement(emptyBlock.entryLabels());
        final String successorLabel = anyElement(emptyBlock.exitLabels());

        final BasicBlock successor = graph.byLabel(successorLabel, Direction.FORWARD).orElseThrow();

        final List<BasicBlock> blocks = new ArrayList<>();
        for (BasicBlock block : graph) {
            if (block == emptyBlock) continue; // Skip the empty point, deleting it.

            if (block == successor)
                updateEntryPoint(block, successorLabel, predecessorLabel);
            blocks.add(block);
        }

        graph.updateNodes(blocks);
        return Set.of(successor);
    }

    private void updateEntryPoint(BasicBlock block, String originalLabel, String replacedLabel) {
        final var instruction = block.entryPoint();
        if (instruction instanceof UnconditionalEntry entry && entry.label.equals(originalLabel)) {
            entry.label = replacedLabel;

        } else if (instruction instanceof ConditionalEntry entry) {
            if (entry.trueLabel.equals(originalLabel)) {
                entry.trueLabel = replacedLabel;
            } else if (entry.falseLabel.equals(originalLabel)) {
                entry.falseLabel = replacedLabel;
            }
        }
    }
}
