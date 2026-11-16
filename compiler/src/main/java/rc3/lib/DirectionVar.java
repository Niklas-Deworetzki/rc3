package rc3.lib;

import rc3.januscompiler.Direction;

import java.util.Objects;
import java.util.function.*;

/**
 * This class provides a mutable container for two generic references: One
 * for every {@link Direction}.
 * <p>
 * This class should be used when two variables represent a single concept
 * with different values depending on the {@link Direction}.
 * Instead of creating two values and naming them with a suffix <code>fw</code>
 * and <code>bw</code>, depending on the direction, this class can be used
 * and accessed via {@link DirectionVar#get(Direction)} and
 * {@link DirectionVar#set(Direction, Object)} methods.
 *
 * @param <V>
 */
public final class DirectionVar<V> {
    private V fw, bw;

    public DirectionVar(V fw, V bw) {
        this.fw = fw;
        this.bw = bw;
    }

    /**
     * Fetches the value stored for one {@link Direction}.
     *
     * @param direction The {@link Direction} for which the value should be returned.
     */
    public V get(Direction direction) {
        return direction.choose(fw, bw);
    }

    /**
     * Updates the value stored for one {@link Direction}.
     *
     * @param direction The {@link Direction} for which the value should be updated.
     * @param value     The updated value.
     */
    public void set(Direction direction, V value) {
        switch (direction) {
            case FORWARD -> this.fw = value;
            case BACKWARD -> this.bw = value;
        }
    }

    /**
     * Updates both values stored in this instance.
     *
     * @param fw The new forward value.
     * @param bw The new backward value.
     */
    public void set(V fw, V bw) {
        this.fw = fw;
        this.bw = bw;
    }

    /**
     * Applies a {@link Function} to the value for every {@link Direction} and returns
     * a new {@link DirectionVar} holding the results.
     * <p>
     * This method does <b>not</b> change the contents of this instance.
     *
     * @param function The {@link Function} to apply to both values.
     */
    public <U> DirectionVar<U> map(Function<? super V, ? extends U> function) {
        return new DirectionVar<>(
                function.apply(fw),
                function.apply(bw));
    }

    /**
     * Applies a {@link Function} to the value for every {@link Direction} and returns
     * a new {@link DirectionVar} holding the results.
     * <p>
     * This method does <b>not</b> change the contents of this instance.
     * <p>
     * In contrast to {@link DirectionVar#map(Function)} this method also passes the
     * {@link Direction} of a value to the given {@link BiFunction function}.
     *
     * @param function The {@link BiFunction function} to apply to both values.
     *                 It accepts the {@link Direction} as well as the value itself.
     */
    public <U> DirectionVar<U> map(BiFunction<Direction, ? super V, ? extends U> function) {
        return new DirectionVar<>(
                function.apply(Direction.FORWARD, fw),
                function.apply(Direction.BACKWARD, bw));
    }

    /**
     * Applies a {@link Function} to the value for every {@link Direction} and returns
     * a new {@link DirectionVar} holding the results.
     * <p>
     * This method does <b>not</b> change the contents of this instance.
     * <p>
     * The {@link Function} to apply is passed as a {@link DirectionVar}, using the
     * forward {@link Function} to apply to the forward value and the backward
     * {@link Function} for the backward value.
     */
    public <U> DirectionVar<U> map(DirectionVar<Function<? super V, ? extends U>> variable) {
        return new DirectionVar<>(
                variable.get(Direction.FORWARD).apply(fw),
                variable.get(Direction.BACKWARD).apply(bw));
    }

    /**
     * Applies a {@link Consumer} to the value for every {@link Direction}.
     *
     * @param action The {@link Consumer} which is applied to both values of this instance.
     */
    public void forEach(Consumer<? super V> action) {
        action.accept(fw);
        action.accept(bw);
    }

    /**
     * Applies a {@link Consumer} to the value for every {@link Direction}.
     * Additional to the value, the direction itself is also passed.
     *
     * @param action The action which is applied to both values of this instance.
     */
    public void forEach(BiConsumer<Direction, ? super V> action) {
        action.accept(Direction.FORWARD, fw);
        action.accept(Direction.BACKWARD, bw);
    }

    /**
     * Applies a {@link Consumer} to the value for every {@link Direction}.
     * The {@link Consumer} is provided as a {@link DirectionVar}. Its
     * forward value is used to consume the forward value of this instance,
     * while the backward value is used to consume the backward value of
     * this instance.
     *
     * @param variable A {@link DirectionVar} holding two {@link Consumer}.
     */
    public void forEach(DirectionVar<Consumer<? super V>> variable) {
        variable.get(Direction.FORWARD).accept(fw);
        variable.get(Direction.FORWARD).accept(bw);
    }

    /**
     * Uses a binary function applied on both values to extract a result.
     */
    public <R> R fold(BiFunction<V, V, R> function) {
        return function.apply(fw, bw);
    }

    /**
     * Creates a {@link DirectionVar} from two values, representing the
     * two {@link Direction directions}.
     *
     * @param fw The value for the forward direction.
     * @param bw The value for the backward direction.
     */
    public static <V> DirectionVar<V> of(V fw, V bw) {
        return new DirectionVar<>(fw, bw);
    }

    /**
     * Creates a {@link DirectionVar} using the given {@link Supplier}
     * twice, to fill the forward and backward value.
     *
     * @param supplier A supplier generating the values for both directions.
     */
    public static <V> DirectionVar<V> of(Supplier<V> supplier) {
        return new DirectionVar<>(supplier.get(), supplier.get());
    }

    /**
     * Creates a {@link DirectionVar} using the given {@link Function}
     * by applying it twice: Once on each {@link Direction}.
     *
     * @param function A function applied to both {@link Direction#FORWARD}
     *                 and {@link Direction#BACKWARD}.
     */
    public static <V> DirectionVar<V> instantiate(Function<Direction, ? extends V> function) {
        return new DirectionVar<>(function.apply(Direction.FORWARD), function.apply(Direction.BACKWARD));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fw, bw);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DirectionVar<?> other)) return false;
        return Objects.equals(this.fw, other.fw) &&
                Objects.equals(this.bw, other.bw);
    }

    @Override
    public String toString() {
        return String.format("DirectionValue[fw=%s,bw=%s]", fw, bw);
    }
}
