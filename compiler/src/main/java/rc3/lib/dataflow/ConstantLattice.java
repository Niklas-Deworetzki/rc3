package rc3.lib.dataflow;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A {@link Lattice} created on top of a domain of values, with {@link ConstantLattice#bottom()}
 * below this domain and {@link ConstantLattice#top()} above this domain.
 *
 * @param <U> The type representing the base domain.
 */
public class ConstantLattice<U> implements Lattice<ConstantLattice.Value<U>> {
    // Static shared instance.
    protected static final ConstantLattice<?> INSTANCE = new ConstantLattice<>();

    /**
     * Returns an instance of the {@link ConstantLattice} class.
     */
    @SuppressWarnings("unchecked")
    public static <U> ConstantLattice<U> instance() {
        return (ConstantLattice<U>) INSTANCE;
    }

    /**
     * Lifts a single value of the base domain into the {@link Lattice}.
     */
    public Constant<U> lift(U value) {
        return new Constant<>(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value<U> top() {
        return (Value<U>) SpecialValues.TOP;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value<U> bottom() {
        return (Value<U>) SpecialValues.BOT;
    }

    /**
     * <table border="1">
     *  <tr>
     *      <td><b><i>&#8852;</i></b></td>
     *      <td><b>T</b></td>
     *      <td><b>x</b></td>
     *      <td><b>&#8869;</b></td>
     *  </tr>
     *  <tr>
     *      <td><b>T</b></td>
     *      <td>T</td>
     *      <td>T</td>
     *      <td>T</td>
     *  </tr>
     *  <tr>
     *      <td><b>x</b></td>
     *      <td>T</td>
     *      <td>x</td>
     *      <td>x</td>
     *  </tr>
     *  <tr>
     *      <td><b>y &#8800; x</b></td>
     *      <td>T</td>
     *      <td>T</td>
     *      <td>y</td>
     *  </tr>
     *  <tr>
     *      <td><b>&#8869;</b></td>
     *      <td>T</td>
     *      <td>x</td>
     *      <td>&#8869;</td>
     *  </tr>
     * </table>
     */
    @Override
    public Value<U> join(Value<U> a, Value<U> b) {
        if (a.equals(top()) || b.equals(top())) {
            return top();
        } else if (a.equals(bottom())) {
            return b;
        } else if (b.equals(bottom())) {
            return a;
        } else if (a.equals(b)) {
            return a;
        } else {
            return top();
        }
    }

    /**
     * <table border="1">
     *  <tr>
     *      <td><b><i>&#8851;</i></b></td>
     *      <td><b>T</b></td>
     *      <td><b>x</b></td>
     *      <td><b>&#8869;</b></td>
     *  </tr>
     *  <tr>
     *      <td><b>T</b></td>
     *      <td>T</td>
     *      <td>x</td>
     *      <td>&#8869;</td>
     *  </tr>
     *  <tr>
     *      <td><b>x</b></td>
     *      <td>x</td>
     *      <td>x</td>
     *      <td>&#8869;</td>
     *  </tr>
     *  <tr>
     *      <td><b>y &#8800; x</b></td>
     *      <td>y</td>
     *      <td>&#8869;</td>
     *      <td>&#8869;</td>
     *  </tr>
     *  <tr>
     *      <td><b>&#8869;</b></td>
     *      <td>&#8869;</td>
     *      <td>&#8869;</td>
     *      <td>&#8869;</td>
     *  </tr>
     * </table>
     */
    @Override
    public Value<U> meet(Value<U> a, Value<U> b) {
        if (a.equals(bottom()) || b.equals(bottom())) {
            return bottom();
        } else if (a.equals(top())) {
            return b;
        } else if (b.equals(top())) {
            return a;
        } else if (a.equals(b)) {
            return a;
        } else {
            return bottom();
        }
    }

    /**
     * Interface providing a common superclass for all {@link Value values} present
     * in the {@link ConstantLattice}. Instances of this interface are either
     * {@link Constant Constants}, {@link ConstantLattice#top() T}, or
     * {@link ConstantLattice#bottom() &#8869;}.
     */
    public sealed interface Value<U> {
        /**
         * Returns the constant value presented by this instance wrapped into
         * an {@link Optional}. If this is {@link ConstantLattice#top() T} or
         * {@link ConstantLattice#bottom() &#8869;}, the returned optional is
         * empty.
         */
        Optional<U> get();

        /**
         * Returns the constant value presented by this instance.
         *
         * @throws NoSuchElementException if this is {@link ConstantLattice#top() T}
         *                                or {@link ConstantLattice#bottom() &#8869;}
         */
        default U unpack() throws NoSuchElementException {
            return get().orElseThrow();
        }

        /**
         * Applies the given {@link Function} on the value represented by
         * this object. If this is {@link ConstantLattice#top() T} or
         * {@link ConstantLattice#bottom() &#8869;}, {@link Optional#empty()}
         * is returned.
         */
        default <R> Optional<R> map(Function<U, R> function) {
            return get().map(function);
        }


        /**
         * Returns <code>true</code>, if this is {@link ConstantLattice#top()}.
         */
        default boolean isTop() {
            return this == SpecialValues.TOP;
        }

        /**
         * Returns <code>true</code>, if this is {@link ConstantLattice#bottom()} ()}.
         */
        default boolean isBottom() {
            return this == SpecialValues.BOT;
        }

        /**
         * Returns <code>true</code>, if this is neither {@link ConstantLattice#top()}
         * nor {@link ConstantLattice#bottom()}.
         */
        default boolean isPresent() {
            return !isTop() && !isBottom();
        }

        /**
         * Returns <code>true</code>, if this {@link Value#isPresent() is present}
         * and the value contained is equal to the argument.
         */
        default boolean isValue(U value) {
            return isPresent() && Objects.equals(unpack(), value);
        }
    }


    private enum SpecialValues implements Value<Object> {
        TOP, BOT;

        @Override
        public Optional<Object> get() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return switch (this) {
                case TOP -> "T";
                case BOT -> "\uF05E";
            };
        }
    }

    public record Constant<T>(T value) implements Value<T> {
        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public Optional<T> get() {
            return Optional.of(value);
        }

        @Override
        public T unpack() throws NoSuchElementException {
            return value; // Efficient implementation doesn't require creation of Optional.
        }
    }
}
