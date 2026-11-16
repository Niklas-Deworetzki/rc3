package rc3.januscompiler;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An enumeration of the two possible directions: {@link Direction#FORWARD} and {@link Direction#BACKWARD}.
 * <p>
 * Since, in reversible computing at least, the execution direction is made explicit,
 * many constructs convey a notion of direction. Therefore, this class provides a means
 * to specify a {@link Direction} more explicitly than using boolean values to encode
 * forward and backward.
 * <p>
 * All uses of directional behavior or data should be represented using this class,
 * since it provides not only an explicit description of direction, but also useful
 * helper methods, to inspect or reason over a direction in an abstract way.
 */
public enum Direction {
    /**
     * The forward direction.
     * <p>
     * This value represents the <i>usual</i> direction of execution or
     * procedure invocations (call).
     */
    FORWARD {
        @Override
        public final <A> A choose(A forward, A backward) {
            return forward;
        }
    },
    /**
     * The backward direction.
     * <p>
     * This value presents the <i>inverted</i> direction of execution
     * or an uncall in the context of a procedure invocation.
     */
    BACKWARD {
        @Override
        public final <A> A choose(A forward, A backward) {
            return backward;
        }
    };

    /**
     * Choose a value depending on the direction.
     *
     * @param forward  The returned value, if this is {@link Direction#FORWARD}.
     * @param backward The returned value, if this is {@link Direction#BACKWARD}.
     */
    public abstract <A> A choose(A forward, A backward);

    /**
     * Choose a value depending on the direction and calculate it.
     * <p>
     * This method is like {@link Direction#choose(Object, Object)}, but does
     * not force evaluation of the not-chosen value.
     */
    public final <R> R chooseLazy(Supplier<? extends R> forward,
                                  Supplier<? extends R> backward) {
        return choose(forward, backward).get();
    }

    /**
     * Variant of the {@link Direction#choose(Object, Object)} method used to
     * access different members of an instance depending on the {@link Direction}.
     * <p>
     * The behavior of this method can be modelled using {@link Direction#choose(Object, Object)}
     * with two {@link Function} objects and a call to {@link Function#apply(Object)}. But this
     * method already fixes the method parameters as a {@link Function} type, aiding Java's type
     * inference when using lambdas.
     *
     * @param forwardAccessor  The {@link Function} used to access the given instance,
     *                         if this is {@link Direction#FORWARD}.
     * @param backwardAccessor The {@link Function} used to access the given instance,
     *                         if this is {@link Direction#BACKWARD}.
     * @param instance         The instance passed to either of the previous two {@link Function Functions}.
     */
    public final <I, R> R chooseAccess(Function<I, ? extends R> forwardAccessor,
                                       Function<I, ? extends R> backwardAccessor,
                                       I instance) {
        return choose(forwardAccessor, backwardAccessor).apply(instance);
    }

    /**
     * Returns the inverted {@link Direction}.
     * <p>
     * If this was {@link Direction#FORWARD}, {@link Direction#BACKWARD} is returned.
     * If this was {@link Direction#BACKWARD}, {@link Direction#FORWARD} is returned.
     */
    public final Direction invert() {
        return choose(BACKWARD, FORWARD);
    }

    /**
     * Returns <code>true</code> if this is {@link Direction#FORWARD}.
     */
    public final boolean isForward() {
        return this == FORWARD;
    }

    /**
     * Returns <code>true</code> if this is {@link Direction#BACKWARD}.
     */
    public final boolean isBackward() {
        return this == BACKWARD;
    }

    /**
     * Returns the call keyword associated with this {@link Direction}.
     * <p>
     * This is either <code>call</code> or <code>uncall</code>.
     */
    public final String asCallKeyword() {
        return choose("call", "uncall");
    }

    /**
     * Returns the {@link String} <code>fw</code> or <code>bw</code> to represent
     * this {@link Direction}.
     */
    public final String asSuffix() {
        return choose("fw", "bw");
    }

    @Override
    public final String toString() {
        return choose("forward", "backward");
    }
}
