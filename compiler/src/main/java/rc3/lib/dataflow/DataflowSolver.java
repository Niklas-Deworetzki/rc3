package rc3.lib.dataflow;

import java.util.Map;

/**
 * Implementing classes of this interface can be used to solve a
 * {@link DataflowProblem}.
 * <p>
 * This interface allows implementing classes to define restrictions
 * for the type <var>N</var> of the nodes in a {@link DataflowProblem}
 * as well as the type <var>L</var> of the {@link Lattice} used in
 * a {@link DataflowProblem}. This is, so an implementation may use
 * either or both these restrictions to find a potentially more
 * optimized internal representation or strategy to solve the problem.
 * <p>
 * The value returned when {@link DataflowSolver#solve(DataflowProblem)
 * solving} a problem must be a {@link Map}, defining a value for
 * every node in the graph.
 * <p>
 * While faster solvers might exist for specific instances of a
 * {@link DataflowProblem}, this interface also provides a default
 * {@link DataflowSolver}, which can be accessed via the static
 * method {@link DataflowSolver#defaultSolver()}.
 *
 * @param <N> A type restriction for nodes in the <i>Graph</i> of a
 *            {@link DataflowProblem}.
 * @param <L> A type restriction for the type of the {@link Lattice}
 *            of a {@link DataflowProblem}.
 */
public interface DataflowSolver<N, L> {

    /**
     * Solves a {@link DataflowProblem}, providing a value of type
     * <var>L</var> for every node in the <i>Graph</i> of the
     * {@link DataflowProblem}.
     */
    DataflowSolution<N, L> solve(DataflowProblem<N, L> problem);

    /**
     * Returns a {@link DataflowSolver} applicable for all
     * {@link DataflowProblem} instances.
     */
    static <N, L> DataflowSolver<N, L> defaultSolver() {
        @SuppressWarnings("unchecked")
        DataflowSolver<N, L> instance = (DataflowSolver<N, L>) WorklistSolver.INSTANCE;
        return instance;
    }
}
