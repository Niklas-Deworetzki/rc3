package rc3.lib.dataflow;

import rc3.lib.utils.CollectionUtils;

import java.util.stream.Stream;

/**
 * A {@link Lattice} is defined in terms of a <i>Partial Order</i>
 * on a <i>Set</i> <var>T</var>.
 * <p>
 * A <i>Partial Order</i> defines a relation <code>&#8804;</code>
 * such that &#8704;x,y,z &#8712; <var>T</var>:
 * <ul>
 *  <li>x &#8804; x <i>(reflexive)</i></li>
 *  <li>x &#8804; y and y &#8804; x implies x = x <i>(asymmetric)</i></li>
 *  <li>x &#8804; y and y &#8804; z implies x &#8804; z <i>(transitive)</i></li>
 * </ul>
 * <p>
 * Within such a <i>Set</i> <var>T</var>, it is possible to define two
 * operations <code>meet</code> (also written <b>&#8851;</b>) and
 * <code>join</code> (also written <b>&#8852;</b>) to compute upper
 * and lower bounds:
 * <ol>
 *  <li><code>join</code> computes the least upper bound.</li>
 *  <li><code>meet</code> computes the greatest lower bound.</li>
 * </ol>
 * <p>
 * If x &#8851; y and x &#8852; y exist for all x,y &#8712; <var>T</var>,
 * then <var>T</var> is a {@link Lattice}.
 * <p>
 * Furthermore a {@link Lattice} defines two special elements top
 * (also written <b>T</b>) and bottom (also written <b>&#8869;</b>),
 * which represent the greatest and lowest element in <var>T</var>
 * respectively.
 * <p>
 * A {@link Lattice} is used to model a {@link DataflowProblem}. If the
 * dataflow equations described by {@link DataflowProblem#transfer(Object, Object)}
 * and the structure of the underlying <i>Graph</i> are modelled to operate
 * on a {@link Lattice}, it is guaranteed that a <b>fixpoint</b> will always
 * exist. Therefore a solution to the {@link DataflowProblem} can always be
 * found.
 *
 * @param <T> Represents the <i>Set</i> in terms of which an instance
 *            of this {@link Lattice} is defined as a type in Java
 *            and therefore the <i>Set</i> of all instances for this
 *            type.
 */
public interface Lattice<T> {

    /**
     * The top element (also written <b>T</b>) is the greatest
     * element in this {@link Lattice}.
     *
     * @return The top element.
     */
    T top();

    /**
     * The bottom element (also written <b>&#8869;</b>) is the
     * least element in this {@link Lattice}.
     *
     * @return The bottom element.
     */
    T bottom();

    /**
     * The join operation (also written <b>&#8852;</b>) computes the
     * <i>least upper bound</i> or <i>supremum</i> of the set {a, b},
     * where <code>a</code> and <code>b</code> are this methods parameters.
     *
     * @return a &#8852; b
     */
    T join(T a, T b);

    /**
     * The meet operation (also written <b>&#8851;</b>) computes the
     * <i>greatest lower bound</i> or <i>infimum</i> of the set {a, b},
     * where <code>a</code> and <code>b</code> are this methods parameters.
     *
     * @return a &#8851; b
     */
    T meet(T a, T b);

    /**
     * Computes the <i>least upper bound</i> of the elements in <var>ts</var>.
     *
     * @return &#8852; ts
     * @see Lattice#join(Object, Object)
     */
    default T joinAll(Stream<T> ts) {
        return ts.reduce(bottom(), this::join);
    }

    /**
     * Computes the <i>least upper bound</i> of the elements in <var>ts</var>.
     *
     * @return &#8852; ts
     * @see Lattice#join(Object, Object)
     */
    default T joinAll(Iterable<T> ts) {
        return joinAll(CollectionUtils.stream(ts));
    }

    /**
     * Computes the <i>greatest lower bound</i> of the elements in <var>ts</var>.
     *
     * @return &#8851; ts
     * @see Lattice#meet(Object, Object)
     */
    default T meetAll(Stream<T> ts) {
        return ts.reduce(top(), this::meet);
    }

    /**
     * Computes the <i>greatest lower bound</i> of the elements in <var>ts</var>.
     *
     * @return &#8851; ts
     * @see Lattice#meet(Object, Object)
     */
    default T meetAll(Iterable<T> ts) {
        return meetAll(CollectionUtils.stream(ts));
    }
}
