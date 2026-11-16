package rc3.rssa.control;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.Program;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * A collection of strategies with the aim to simplify the structure of a {@link ControlGraph}.
 */
public class ControlGraphSimplification extends Program.ProcedureOptimization {

    private final List<SimplificationAlgorithm> SUPPORTED_SIMPLIFICATIONS = List.of(
            new MergeStrictlyConsecutiveBlocks(),
            new EliminateEmptyBlocks()
    );

    public ControlGraphSimplification(Compiler compiler) {
        super(AvailableOptimization.SIMPLIFY_CONTROL_FLOW, compiler);
    }

    private void applySimplification(SimplificationAlgorithm simplificationAlgorithm, ControlGraph graph) {
        final Queue<BasicBlock> blocksToProcess = new ArrayDeque<>(graph.getNodes());

        while (!blocksToProcess.isEmpty()) {
            final BasicBlock block = blocksToProcess.poll();
            if (graph.getNodes().contains(block) && simplificationAlgorithm.isApplicable(graph, block)) {
                final var updated = simplificationAlgorithm.apply(graph, block);
                blocksToProcess.addAll(updated);
            }
        }
    }

    @Override
    public ControlGraph apply(String s, ControlGraph graph) {
        for (SimplificationAlgorithm supportedSimplification : SUPPORTED_SIMPLIFICATIONS) {
            applySimplification(supportedSimplification, graph);
        }
        return graph;
    }


    /**
     * Interface for different strategies to simplify the structure of a {@link ControlGraph}.
     */
    interface SimplificationAlgorithm {
        /**
         * Tests whether the simplification is applicable for the given {@link BasicBlock}.
         */
        boolean isApplicable(ControlGraph graph, BasicBlock block);

        /**
         * Applies the simplification and returns a {@link Set} of {@link BasicBlock}s
         * affected by the simplification.
         */
        Set<BasicBlock> apply(ControlGraph graph, BasicBlock block);
    }
}
