package rc3.lib.dataflow;

import rc3.lib.utils.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * A {@link DataflowSolver} implementation, that operates on each node of the
 * {@link DataflowProblem}'s graph individually, while keeping track of nodes,
 * that require (re-)processing in a <i>worklist</i>.
 */
final class WorklistSolver<N, L> implements DataflowSolver<N, L> {
    static final WorklistSolver<?, ?> INSTANCE = new WorklistSolver<>();

    /**
     * Returns all nodes, whose value must be used as an input for the
     * dataflow equation for the given node.
     */
    private Iterable<N> equationInputNodesFor(DataflowProblem<N, L> problem, N node) {
        if (problem.getDirection().isForward()) {
            return problem.getPredecessors(node);
        } else {
            return problem.getSuccessors(node);
        }
    }

    /**
     * Returns all nodes, which are affected by the output of the dataflow
     * equation for the given node.
     */
    private Iterable<N> equationOutputNodeFor(DataflowProblem<N, L> problem, N node) {
        if (problem.getDirection().isForward()) {
            return problem.getSuccessors(node);
        } else {
            return problem.getPredecessors(node);
        }
    }

    @Override
    public DataflowSolution<N, L> solve(DataflowProblem<N, L> problem) {
        final Map<N, L> equationInValues = new HashMap<>();
        final Map<N, L> equationOutValues = new HashMap<>();

        // Initialize with default values.
        if (problem.getDirection().isForward()) {
            for (N node : problem) {
                equationInValues.put(node, problem.initialInValue(node));
                equationOutValues.put(node, problem.initialOutValue(node));
            }
        } else {
            for (N node : problem) {
                equationInValues.put(node, problem.initialOutValue(node));
                equationOutValues.put(node, problem.initialInValue(node));
            }
        }

        // Initialize the worklist with all nodes of the graph.
        final Queue<N> worklist = new LinkedList<>(problem.elements());

        while (!worklist.isEmpty()) {
            final N node = worklist.poll();

            final L equationInput = CollectionUtils.stream(equationInputNodesFor(problem, node))
                    .map(equationOutValues::get)
                    .reduce(equationInValues.get(node), problem::combine);
            equationInValues.put(node, equationInput);

            final L equationOutput = problem.transfer(equationInput, node);
            if (!equationOutput.equals(equationOutValues.put(node, equationOutput))) {
                for (final N nodeAffectedByOutput : equationOutputNodeFor(problem, node)) {
                    worklist.add(nodeAffectedByOutput);
                }
            }
        }

        if (problem.getDirection().isForward()) {
            return new DataflowSolution<>(equationInValues, equationOutValues);
        } else {
            return new DataflowSolution<>(equationOutValues, equationInValues);
        }
    }
}
