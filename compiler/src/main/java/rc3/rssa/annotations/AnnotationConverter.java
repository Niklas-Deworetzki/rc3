package rc3.rssa.annotations;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Instances of this class are used to implicitly convert values for an {@link AnnotationField}.
 */
public interface AnnotationConverter extends Predicate<Object>, Function<Object, Object> {

    /**
     * Checks whether a given value can be converted by this {@link AnnotationConverter}.
     *
     * @return <code>true</code> if this instance can convert the given value.
     */
    boolean acceptsValue(Object value);

    /**
     * Converts a given value.
     */
    Object convertValue(Object value);

    @Override
    default Object apply(Object o) {
        if (acceptsValue(o)) return convertValue(o);
        throw new UnsupportedOperationException("Converter does not accept " + o);
    }

    @Override
    default boolean test(Object o) {
        return acceptsValue(o);
    }

    /**
     * This default {@link AnnotationConverter} class can be used to convert instances of a special class
     * using a fixed function.
     * <p>
     * It accepts all instances of a {@link Class} and converts them using the function provided in the constructor.
     */
    record ClassBasedConverter<T, R>(Class<T> acceptingType,
                                     Function<T, R> conversionFunction) implements AnnotationConverter {

        @Override
        public boolean acceptsValue(Object value) {
            return acceptingType.isInstance(value);
        }

        @Override
        public Object convertValue(Object value) {
            return conversionFunction.apply(acceptingType.cast(value));
        }
    }

    /**
     * This default {@link AnnotationConverter} class can be used to convert {@link String} values describing
     * an {@link Enum} field to the {@link Enum} value.
     */
    record EnumConverter<E extends Enum<E>>(Class<E> enumClass) implements AnnotationConverter {

        private Optional<E> findEnumField(String name) {
            for (E enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equalsIgnoreCase(name)) {
                    return Optional.of(enumConstant);
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean acceptsValue(Object value) {
            if (enumClass.isInstance(value)) return false; // Do not try to convert enum values.
            return findEnumField(value.toString()).isPresent();
        }

        @Override
        public Object convertValue(Object value) {
            return findEnumField(value.toString()).orElseThrow();
        }
    }
}
