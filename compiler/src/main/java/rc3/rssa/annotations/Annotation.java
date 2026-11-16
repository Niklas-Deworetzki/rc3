package rc3.rssa.annotations;

import rc3.lib.parsing.Position;
import rc3.rssa.annotations.views.*;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Annotation of the form <code>@Identifier(key=value, ...)</code>.
 * <p>
 * {@link Annotation Annotations} are used to enrich the capabilities of RSSA in order to support
 * features, that are outside the scope of regular RSSA code or provide additional information for
 * executed {@link Instruction Instructions}.
 * <p>
 * Currently, {@link Annotation Annotations} are used to:
 * <ul>
 *  <li>Mark the original source position in a compiled program, to aid the programmer while debugging.</li>
 *  <li>Perform bound checks when accessing an array via an index.</li>
 *  <li>Manage memory via <code>@Memory</code>, <code>@Heap</code> and <code>@Allocation</code>
 *  {@link Annotation Annotations}. They are used to initialize, request and mutate the Heap.</li>
 *  <li>Control the program execution. <code>@Entry</code> marks an optional entry point for a program,
 *  while <code>@Param</code> is used to format results of a program after execution.</li>
 * </ul>
 * <p>
 * As {@link Annotation Annotations} may come in various shapes and usages, we aimed to make them
 * as generic as possible to aid future extensions of the {@link Annotation} mechanism.
 * Therefore, the {@link Annotation} class only <i>knows</i> about the {@link Annotation Annotations}
 * identifier as well as the key-value-pairs of an {@link Annotation}. Information about the
 * semantics of an {@link Annotation} is abstracted away into {@link AnnotationView} and
 * {@link AnnotationHandle} classes.
 * <p>
 * An {@link AnnotationView} provides a usable interface for a programmer to interact with an
 * {@link Annotation}. The {@link AnnotationView} can be obtained via the method
 * {@link Annotation#getView()}, which returns an {@link AnnotationView} casted into a
 * generic, user-provided sub-type.
 * <p>
 * The {@link AnnotationHandle} provides the {@link AnnotationView} for a given {@link Annotation},
 * as well as a well-defined structure of an {@link Annotation}. {@link AnnotationHandle Handles}
 * are associated via an {@link Annotation Annotations} identifier.
 */
public final class Annotation {
    // Map of all registered handles.
    private static final Map<String, AnnotationHandle> HANDLES = new HashMap<>();

    static {
        Annotation.registerUnformattedHandle("Source", SourceView::new,
                AnnotationField.requiredField("position", Position.class));

        Annotation.registerHandle("InBounds", InBoundsView::new,
                AnnotationField.requiredField("base", Atom.class),
                AnnotationField.requiredField("size", Atom.class));

        Annotation.registerHandle("Allocation", AllocationView::new,
                AnnotationField.requiredField("usage", RValue.class),
                AnnotationField.requiredField("request", Constant.class));

        Annotation.registerHandle("Heap", HeapView::new,
                AnnotationField.requiredField("size", Constant.class));

        Annotation.registerHandle("Memory", MemoryView::new,
                AnnotationField.requiredField("address", Constant.class),
                AnnotationField.requiredField("value", Constant.class));

        Annotation.registerHandle("Param", ParamView::new,
                AnnotationField.optionalEnumField("type", ParamView.Type.class),
                AnnotationField.optionalField("value", Constant.class),
                AnnotationField.optionalField("name", String.class),
                AnnotationField.optionalField("hidden", Boolean.class),
                AnnotationField.optionalField("size", Atom.class));

        Annotation.registerHandle("Entry", AnnotationView::new,
                AnnotationField.requiredField("value", String.class));

    }

    /**
     * Registers a new type of {@link Annotation}.
     *
     * @param identifier      The identifier (name) used for this {@link Annotation}. If an {@link Annotation} has
     *                        the form <code>@identifier</code> with <code>identifier</code> being equal to the
     *                        passed value, it is associated with the handle created by this method.
     * @param viewConstructor The constructor of the {@link AnnotationView} class to associate with the given
     *                        type of {@link Annotation}.
     * @param fields          An enumeration of available fields for the given type of {@link Annotation}.
     * @throws IllegalStateException If another handle was already registered for the given type of {@link Annotation}.
     */
    public static void registerHandle(String identifier, Function<Annotation, ? extends AnnotationView> viewConstructor,
                                      AnnotationField... fields) throws IllegalStateException {
        registerHandle(identifier, new AnnotationHandle(Arrays.asList(fields), viewConstructor, false));
    }

    /**
     * Registers a new type of {@link Annotation} and marks it as <i>unformatted</i>. Instances of this
     * type are not included when creating a {@link String} representation for an {@link Annotation}.
     *
     * @param identifier      The identifier (name) used for this {@link Annotation}. If an {@link Annotation} has
     *                        the form <code>@identifier</code> with <code>identifier</code> being equal to the
     *                        passed value, it is associated with the handle created by this method.
     * @param viewConstructor The constructor of the {@link AnnotationView} class to associate with the given
     *                        type of {@link Annotation}.
     * @param fields          An enumeration of available fields for the given type of {@link Annotation}
     * @throws IllegalStateException If another handle was already registered for the given type of {@link Annotation}.
     */
    public static void registerUnformattedHandle(String identifier, Function<Annotation, ? extends AnnotationView> viewConstructor,
                                                 AnnotationField... fields) throws IllegalStateException {
        registerHandle(identifier, new AnnotationHandle(Arrays.asList(fields), viewConstructor, true));
    }

    /**
     * Registers a new type of {@link Annotation} with a given {@link AnnotationHandle}.
     *
     * @param identifier The identifier (name) used for this {@link Annotation}. If an {@link Annotation} has
     *                   the form <code>@identifier</code> with <code>identifier</code> being equal to the
     *                   passed value, it is associated with the handle created passed to this method.
     * @param handle     The handle to register for the given type of {@link Annotation}.
     * @throws IllegalStateException If another handle was already registered for the given type of {@link Annotation}.
     */
    public static void registerHandle(String identifier, AnnotationHandle handle) throws IllegalStateException {
        if (HANDLES.containsKey(identifier)) {
            throw new IllegalStateException("Ambiguous reference to @" + identifier + " annotation registered!");
        }

        HANDLES.put(identifier, handle);
    }

    private final String identifier;
    private final Map<String, Object> data;

    private final AnnotationHandle handle;

    /**
     * Creates a new {@link Annotation} with given identifier and date.
     * <p>
     * Creating a new {@link Annotation} will also validate its state, checking the
     * validity of given key-value-pairs. If a required key is missing or a value
     * does not match the required type, this {@link Annotation} is not valid and an
     * {@link IllegalStateException} is thrown.
     *
     * @param identifier The identifier of the created {@link Annotation}.
     * @param data       The key-value-pairs present in the {@link Annotation}.
     * @throws NullPointerException  If the given identifier is <code>null</code>.
     * @throws IllegalStateException If invalid key-value-pairs are passed to this constructor.
     */
    public Annotation(String identifier, Map<String, Object> data) throws NullPointerException, IllegalStateException {
        this.identifier = requireNonNull(identifier, "Identifier of an Annotation cannot be null!");
        this.data = new HashMap<>(data); // Create copy HashMap to ensure mutability.
        this.handle = lookupHandle();

        validateState();
    }

    // Look up the handle associated with this Annotation's type. Use default handle if none is present.
    private AnnotationHandle lookupHandle() {
        if (!HANDLES.containsKey(identifier)) {
            return AnnotationHandle.defaultHandle();
        }

        return HANDLES.get(identifier);
    }

    private void validateState() throws IllegalStateException {
        // TODO: Warn if annotation contains unknown fields?

        for (AnnotationField field : handle.getFields()) {
            Object value = data.get(field.getKey());

            if (field.isRequired() && value == null) {
                throw new IllegalStateException(
                        String.format("Annotation '%s' is missing required field '%s'.",
                                identifier, field.getKey()));
            }

            if (value != null) {
                // Try to perform implicit conversions for the Annotation.
                Optional<Object> conversion = field.implicitlyConvert(value);
                if (conversion.isPresent()) {
                    // Updated value stored for Annotation as conversion was successful.
                    value = conversion.get();
                    data.put(field.getKey(), value);
                }

                if (!field.getEnforcedType().isInstance(value)) {
                    throw new IllegalStateException(
                            String.format("Annotation '%s' field '%s' must be of type %s but was %s.",
                                    identifier, field.getKey(), field.getEnforcedType(), value.getClass()));
                }
            }

        }
    }

    /**
     * Creates a {@link Predicate} that only accepts {@link Annotation Annotations} with
     * the given identifier.
     */
    public static Predicate<Annotation> withIdentifier(String identifier) {
        return annotation -> annotation.hasIdentifier(identifier);
    }

    /**
     * Checks whether this {@link Annotation} has the given identifier.
     *
     * @return <code>true</code> if the identifier of this {@link Annotation} equals the
     * given identifier.
     */
    public boolean hasIdentifier(String identifier) {
        return this.identifier.equals(identifier);
    }

    /**
     * Returns a reference to the raw data of this {@link Annotation}.
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Returns the identifier of this {@link Annotation}.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the {@link AnnotationHandle} associated with this {@link Annotation}.
     */
    public AnnotationHandle getHandle() {
        return handle;
    }

    /**
     * Returns an instance of the {@link AnnotationView} associated with this {@link Annotation}.
     * <p>
     * The {@link AnnotationView View} is casted into the type requested for this method.
     *
     * @param <V> The return type to which the {@link AnnotationView} should be casted to.
     */
    public <V extends AnnotationView> V getView() {
        return getHandle().constructView(this);
    }

    /**
     * Returns the value provided for one of this {@link Annotation Annotations} parameters.
     * This is a shortcut for
     * <pre>
     *   this.getView().get(key)
     * </pre>
     *
     * @param key The name of the parameter.
     * @param <T> The type of the value.
     */
    public <T> T get(String key) {
        return getView().get(key);
    }

    /**
     * Used to update all values in the key-value-pairs of this {@link Annotation} with a generic
     * {@link Function}.
     *
     * @param predicateClass The {@link Class} for which the function is applicable.
     * @param function       The function used to transform the values.
     * @return The key-value-pairs also present in this {@link Annotation} but with updated values.
     */
    private <P> Map<String, Object> updateInstanceValues(Class<P> predicateClass, Function<P, ?> function) {
        Map<String, Object> result = new HashMap<>(data.size());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (predicateClass.isInstance(entry.getValue())) {
                result.put(entry.getKey(), function.apply(predicateClass.cast(entry.getValue())));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Returns a new {@link Annotation} where all occurrences of the given {@link Variable} have been
     * replaced with the given {@link Atom}.
     */
    public Annotation replaceVariable(Variable original, Atom replacement) {
        return new Annotation(identifier, updateInstanceValues(Variable.class,
                variable -> variable.replace(original, replacement)));
    }

    /**
     * Creates a copy of this {@link Annotation} and all its members.
     */
    public Annotation copy() {
        return new Annotation(identifier, updateInstanceValues(Variable.class, Variable::copy));
    }

    /**
     * Reverses this {@link Annotation} returning a new {@link Annotation} with the inverse meaning
     * or a copy of this Annotation if it is not reversible.
     */
    public Annotation reverse() {
        AnnotationView view = getView();
        if (!(view instanceof AnnotationView.ReversibleView)) {
            return this.copy();
        }
        return ((AnnotationView.ReversibleView) view).reversed();
    }

    /**
     * Returns a {@link List} of {@link Variable Variables} used in this {@link Annotation}. These Variables
     * have to be known when assigning version numbers to different variables.
     */
    public List<Variable> variablesUsed() {
        return data.values().stream()
                .filter(Variable.class::isInstance)
                .map(Variable.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * This method is used to decide whether or not an {@link Annotation} should be shown when
     * {@link Annotatable#formatAnnotations()} creates a formatted String representation of annotations.
     * <p>
     * If this method returns <code>true</code>, this {@link Annotation} is excluded from the shown annotations.
     */
    boolean shouldHideFormat() {
        return getHandle().shouldHideFormat();
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Annotation other)) {
            return false;
        }

        return this.identifier.equals(other.identifier) &&
                this.data.equals(other.data);
    }

    @Override
    public String toString() {
        return getView().formatAnnotation();
    }

}






