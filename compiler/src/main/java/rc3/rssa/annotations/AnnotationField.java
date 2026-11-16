package rc3.rssa.annotations;

import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Constant;
import rc3.rssa.instances.Variable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class is used to describe a <i>field</i> (key-value-pair) of an {@link Annotation}.
 * <p>
 * An {@link AnnotationField} is described by the <b>key</b>, an identifier that is used to
 * associate a value with a special meaning within an {@link Annotation}. This key is
 * used to reference this field.
 * <p>
 * Additionally, an {@link AnnotationField} determines, whether or not a key is required
 * in an {@link Annotation}. If a required field is missing from an {@link Annotation}, this
 * is considered an error. While <i>not</i> required (i.e. optional) fields may be missing.
 * <p>
 * As Java is a typed language, we aimed to make fields typed as well. Therefore a field holds
 * a {@link Class} instance, that is used to identify which instances are allowed as values for
 * this field.
 * With use of the <a href="https://www.oracle.com/technical-resources/articles/java/javareflection.html">
 * Java Reflection API</a>, these type bounds are enforced at runtime, whenever an {@link Annotation}
 * is created.
 * <p>
 * To aid usability when creating instances of an {@link Annotation} (either programmatically or manually
 * in RSSA source code), a list of {@link AnnotationConverter} instances can be provided for
 * an {@link AnnotationField}. These converters accept instances of a given class or with given
 * characteristics and convert them into another value. This way, automatic type conversions can be performed
 * (e.g. {@link Integer} to {@link Constant}) and custom literals can be supported.
 */
public record AnnotationField(Class<?> enforcedType,
                              List<AnnotationConverter> converters,
                              String key, boolean isRequired) {
    /**
     * Creates a new {@link AnnotationField}.
     *
     * @param enforcedType Values for this field must be instances of this class.
     * @param converters   A List of converters used to implicitly convert values for this field.
     * @param key          The key used to identify this field.
     * @param isRequired   Whether this field is required.
     */
    public AnnotationField {
    }

    /**
     * Tries to apply implicit conversions for the given value.
     * <p>
     * All conversions are applied in the order they are defined in. If a conversion
     * was successful, all succeeding conversions are applied for the result of the conversion.
     *
     * @param value The value to convert.
     * @return An empty {@link Optional} if no converter was applicable. Otherwise the {@link Optional}
     * will hold the result of conversion.
     */
    public Optional<Object> implicitlyConvert(Object value) {
        boolean successful = false;

        for (AnnotationConverter converter : getConverters()) {
            if (converter.acceptsValue(value)) {
                successful = true;
                value = converter.convertValue(value);
            }
        }

        if (successful) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns <code>true</code> if this field is required. Otherwise this field is optional.
     */
    @Override
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Returns the key used to identify this field.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the {@link Class} that is used to verify values used for this field. All values
     * used must be an instance of this {@link Class}.
     */
    public Class<?> getEnforcedType() {
        return enforcedType;
    }

    /**
     * Returns the {@link List} of {@link AnnotationConverter AnnotationConverters} for this field.
     */
    public List<AnnotationConverter> getConverters() {
        return converters;
    }

    @Override
    public String toString() {
        String modifier = (isRequired()) ? "required" : "optional";
        return String.format("%s field %s (%s) with %d converters",
                modifier, getKey(), getEnforcedType().getCanonicalName(), getConverters().size());
    }

    /**
     * Creates a new required field.
     */
    public static AnnotationField requiredField(String identifier, Class<?> fieldType) {
        return new AnnotationField(fieldType, getDefaultConverters(fieldType), identifier, true);
    }

    /**
     * Creates a new required field, whose values must be instances of an {@link Enum}.
     * <p>
     * This also creates an {@link AnnotationConverter} to convert from {@link Enum} names to {@link Enum}
     * instances.
     */
    public static <E extends Enum<E>> AnnotationField requiredEnumField(String identifier, Class<E> fieldEnumType) {
        return new AnnotationField(fieldEnumType, List.of(new AnnotationConverter.EnumConverter<>(fieldEnumType)),
                identifier, true);
    }

    /**
     * Creates a new optional field.
     */
    public static AnnotationField optionalField(String identifier, Class<?> fieldType) {
        return new AnnotationField(fieldType, getDefaultConverters(fieldType), identifier, false);
    }

    /**
     * Creates a new optional field, whose values must be instances of an {@link Enum}.
     * <p>
     * This also creates an {@link AnnotationConverter} to convert from {@link Enum} names to {@link Enum}
     * instances.
     */
    public static <E extends Enum<E>> AnnotationField optionalEnumField(String identifier, Class<E> fieldEnumType) {
        return new AnnotationField(fieldEnumType, List.of(
                variableToStringConverter, // Convert symbols to String and then search for enum.
                new AnnotationConverter.EnumConverter<>(fieldEnumType)),
                identifier, false);
    }

    // Converters for Atom or subclasses, that convert Integer and Boolean to Constant.
    private static List<AnnotationConverter> getDefaultConverters(Class<?> fieldType) {
        if (Atom.class.isAssignableFrom(fieldType)) {
            return List.of(intToConstantConverter, booleanToConstantConverter);
        } else if (Boolean.class.equals(fieldType)) {
            return List.of(variableToBooleanConverter, constantToBooleanConverter);
        } else if (String.class.equals(fieldType)) {
            return List.of(variableToStringConverter);
        } else {
            return Collections.emptyList();
        }
    }

    private static final AnnotationConverter intToConstantConverter =
            new AnnotationConverter.ClassBasedConverter<>(Integer.class, Constant::new);

    private static final AnnotationConverter booleanToConstantConverter =
            new AnnotationConverter.ClassBasedConverter<>(Boolean.class, value -> new Constant(value ? 1 : 0));

    private static final AnnotationConverter variableToBooleanConverter =
            new AnnotationConverter.ClassBasedConverter<>(Variable.class, variable -> Boolean.parseBoolean(variable.getRawName()));

    private static final AnnotationConverter constantToBooleanConverter =
            new AnnotationConverter.ClassBasedConverter<>(Constant.class, constant -> constant.value == 1);

    private static final AnnotationConverter variableToStringConverter =
            new AnnotationConverter.ClassBasedConverter<>(Variable.class, Variable::getRawName);
}
