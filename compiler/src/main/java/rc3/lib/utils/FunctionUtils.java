package rc3.lib.utils;

import java.util.function.Function;

public final class FunctionUtils {
    private FunctionUtils() {
        throw new UnsupportedOperationException();
    }

    public static <A, B, C> Function<A, C> compose(Function<B, C> f, Function<A, B> g) {
        return f.compose(g);
    }

    public static <A, B, C> Function<A, C> andThen(Function<A, B> f, Function<B, C> g) {
        return f.andThen(g);
    }

}
