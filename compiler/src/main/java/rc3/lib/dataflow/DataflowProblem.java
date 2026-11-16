package rc3.lib.dataflow;

import rc3.januscompiler.Direction;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * This class is used to describe dataflow problems, as they are usually described using a
 * dataflow equation on a graph. The actual description of a problem is done by creating
 * a concrete subclass and defining the {@link DataflowProblem#transfer(Object, Object)}
 * method, which describes the <i>Transfer Function</i>.
 * <p>
 * Instances of this class are parameterized in regards to <b>3</b> different properties:
 * <ul>
 *  <li>
 *      A {@link Lattice} on which the problem is defined. This {@link Lattice} defines how
 *      different values within the problem interact.
 *  </li>
 *  <li>
 *      A <i>Graph</i> on which the problem is defined. The <i>Graph</i> describes the
 *      relationship between nodes and therefore defines which values interact with
 *      each other. The <i>Graph</i> is given as:
 *      <ol>
 *          <li>A {@link Collection} of nodes.</li>
 *          <li>A {@link Function} yielding the successors of a node.</li>
 *          <li>A {@link Function} yielding the predecessors of a node.</li>
 *      </ol>
 *  </li>
 *  <li>
 *      The actual properties of the dataflow problem. It describes in which direction
 *      information flows within the graph and which operations of the {@link Lattice}
 *      are used whenever two paths collide.
 *  </li>
 * </ul>
 * <p>
 * A result for the problem described by one instance of this class can be obtained
 * using the {@link DataflowProblem#solve()} method, which uses a {@link DataflowSolver}
 * to produce a {@link Map} of results, holding a value for every node in the <i>Graph</i>.
 * <p>
 * The {@link Map} returned by this method is <b>not cached</b> in either way and has
 * to be recalculated whenever it is called.
 * <p>
 * The {@link DataflowProblem#solve(DataflowSolver)} method allows to configure the
 * solving process by specifying a concrete {@link DataflowSolver Solver}. If this
 * argument is omitted, {@link DataflowSolver#defaultSolver()} is used.
 *
 * @param <N> The type parameter describing the nodes of the graph, over which
 *            this dataflow equation should be solved.
 * @param <L> The type over which a lattice is constructed to model interactions
 *            of data.
 */
public abstract class DataflowProblem<N, L> implements Iterable<N> {
    private final Collection<N> graphNodes;
    private final Function<N, ? extends Iterable<N>> predecessors, successors;

    private final Direction direction;

    public DataflowProblem(Collection<N> graphNodes,
                           Function<N, ? extends Iterable<N>> graphPredecessors,
                           Function<N, ? extends Iterable<N>> graphSuccessors,
                           Direction direction) {
        this.graphNodes = graphNodes;
        this.predecessors = graphPredecessors;
        this.successors = graphSuccessors;
        this.direction = direction;
    }

    /**
     * Returns the initial IN value for a given node.
     * <p>
     * If this {@link DataflowProblem} uses a {@link Lattice} to model the underlying operations,
     * this method should return {@link Lattice#top()} or {@link Lattice#bottom()}, depending on
     * the problem, such that a <i>neutral</i> value is used.
     */
    public abstract L initialInValue(N node);

    /**
     * Returns the initial OUT value for a given node.
     * <p>
     * If this {@link DataflowProblem} uses a {@link Lattice} to model the underlying operations,
     * this method should return {@link Lattice#top()} or {@link Lattice#bottom()}, depending on
     * the problem, such that a <i>neutral</i> value is used.
     */
    public abstract L initialOutValue(N node);

    /**
     * Combines the values of two predecessor nodes into a single input to be used for the
     * {@link DataflowProblem#transfer(Object, Object) transfer function}.
     * <p>
     * If {@link DataflowProblem#getDirection() the direction of this DataflowProblem} is
     * {@link Direction#FORWARD}, this method is used to combine <var>OUT<sub>p</sub></var>
     * for all predecessors <var>p</var> of a node <var>n</var>, to compute <var>IN<sub>n</sub></var>.
     * If the direction is {@link Direction#BACKWARD} instead, <var>OUT<var>n</var></var>
     * is computed from <var>IN<sub>s</sub></var> for all successors <var>s</var> of <var>n</var>.
     * <p>
     * If this {@link DataflowProblem} uses a {@link Lattice} to model the underlying operations,
     * this method should be implemented using {@link Lattice#join(Object, Object)} or
     * {@link Lattice#meet(Object, Object)}, depending on the problem.
     */
    public abstract L combine(L a, L b);

    /**
     * This method provides the <i>Transfer Function</i> of this {@link DataflowProblem}.
     * <p>
     * The transfer function is used to compute the value of a node <var>n</var>. If
     * {@link DataflowProblem#getDirection() the direction of this DataflowProblem} is
     * {@link Direction#FORWARD}, this method computes <var>OUT<sub>n</sub></var>
     * from <var>IN<sub>n</sub></var>. If the direction is {@link Direction#BACKWARD},
     * <var>IN<sub>n</sub></var> is computed from <var>OUT<sub>n</sub></var> instead.
     * <p>
     * The value passed as a parameter to this method results from
     * {@link DataflowProblem#combine(Object, Object) combining} the values of all predecessors
     * or successors (depending on direction).
     *
     * @param value The input resulting from the output of all predecessors (or
     *              successors, if in reverse direction) of a node.
     * @param node  A node of the graph, for which this transfer function is used.
     * @return An output for the given node under the given input.
     */
    public abstract L transfer(L value, N node);

    /**
     * Uses the given {@link DataflowSolver Solver} to create a result for this
     * {@link DataflowProblem problem}.
     */
    public DataflowSolution<N, L> solve(DataflowSolver<N, L> solver) {
        return solver.solve(this);
    }

    /**
     * Uses a {@link DataflowSolver default solver} to create a result for this
     * {@link DataflowProblem problem}.
     *
     * @see DataflowSolver#defaultSolver()
     */
    public DataflowSolution<N, L> solve() {
        return solve(DataflowSolver.defaultSolver());
    }

    /**
     * The size of a {@link DataflowProblem problem} is described by the amounts
     * of nodes, which in turn determine the number of equations to solve.
     * <p>
     * This method returns the amount of nodes in the underlying <i>Graph</i>.
     */
    public final int size() {
        return graphNodes.size();
    }

    @Override
    public final Iterator<N> iterator() {
        return graphNodes.iterator();
    }

    /**
     * Returns all nodes in the underlying <i>Graph</i>.
     */
    public final Collection<N> elements() {
        return graphNodes;
    }

    /**
     * Returns the direction in which values are propagated in this
     * {@link DataflowProblem problem}.
     * <p>
     * If the direction is {@link Direction#FORWARD}, a value
     * is passed to the successors of a node. On the other hand, if it is
     * {@link Direction#BACKWARD} values are passed to predecessors
     * instead.
     */
    public final Direction getDirection() {
        return direction;
    }

    /**
     * Returns the predecessors of a given node.
     */
    public final Iterable<N> getPredecessors(N n) {
        return predecessors.apply(n);
    }

    /**
     * Returns the successors of a given node.
     */
    public final Iterable<N> getSuccessors(N n) {
        return successors.apply(n);
    }

}
