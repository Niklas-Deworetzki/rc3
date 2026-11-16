package rc3.rssa.control;

import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;

import java.util.Set;

import static rc3.lib.utils.CollectionUtils.anyElement;
import static rc3.lib.utils.SetOperations.intersects;

/**
 * If an unconditional exit leads to an unconditional entry, both {@link BasicBlock blocks}
 * are always executed after one another. This {@link ControlGraphSimplification.SimplificationAlgorithm} merges such
 * blocks, removing the redundant jump performed by exiting one block and beginning a new one.
 */
public class MergeStrictlyConsecutiveBlocks implements ControlGraphSimplification.SimplificationAlgorithm {
    @Override
    public boolean isApplicable(ControlGraph graph, BasicBlock block) {
        return hasUnconditionalExit(block) &&
                hasUnconditionalEntry(anyElement(graph.getSuccessors(block))) &&
                isNotOwnSuccessor(block);
    }

    private boolean hasUnconditionalExit(BasicBlock block) {
        return block.exitPoint().isUnconditional();
    }

    private boolean hasUnconditionalEntry(BasicBlock block) {
        return block.entryPoint().isUnconditional();
    }

    private boolean isNotOwnSuccessor(BasicBlock block) {
        return !intersects(block.entryLabels(), block.exitLabels());
    }

    @Override
    public Set<BasicBlock> apply(ControlGraph graph, BasicBlock block) {
        final var joined = graph.join(block, anyElement(graph.getSuccessors(block)));
        return Set.of(joined);
    }
}
