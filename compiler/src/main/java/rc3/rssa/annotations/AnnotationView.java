package rc3.rssa.annotations;

import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Instruction;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This class provides basic methods to interact with an {@link Annotation}.
 * <p>
 * To define more specific semantics for an {@link Annotation}, this class should
 * be specialized into a concrete subclass which should then be registered for
 * an {@link Annotation}.
 * <p>
 * This class also provides an Interface {@link ReversibleView} which can be used
 * as a mixin to {@link AnnotationView AnnotationViews} to mark them reversible.
 */
public class AnnotationView {
    private final Annotation annotation;

    public AnnotationView(Annotation annotation) {
        this.annotation = annotation;
    }

    /**
     * Returns the {@link Annotation} for which a view is provided for.
     */
    public final Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Returns a value from the underlying {@link Annotation} and casts it to the
     * requested result type.
     * <p>
     * This method returns <code>null</code> if the key is not present.
     *
     * @param name The key under which a value is stored in the underlying {@link Annotation}.
     * @param <T>  The type to which the fetched result should be cast to.
     * @return The value stored in the underlying {@link Annotation} for the given key
     * cast into the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) annotation.getData().get(name);
    }

    /**
     * Returns a value from the underlying {@link Annotation} and casts it to the
     * requested result type.
     * <p>
     * This method returns a default value provided by a {@link Supplier}, if the
     * key is not present.
     *
     * @param name         The key under which a value is stored in the underlying
     *                     {@link Annotation}.
     * @param defaultValue A {@link Supplier} used to compute a default value, if
     *                     the key is not present.
     * @param <T>          The type to which the fetched result should be cast to.
     * @return The value stored in the underlying {@link Annotation} for the given
     * key cast into the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String name, Supplier<T> defaultValue) {
        if (annotation.getData().containsKey(name)) {
            return (T) annotation.getData().get(name);
        }
        return defaultValue.get();
    }

    /**
     * Provides generic formatting for an {@link Annotation} by re-building the original
     * {@link Annotation} layout.
     * <p>
     * This method starts by producing an <code>'@'</code> character followed by the
     * {@link Annotation Annotations} identifier.
     * Afterwards between opening <code>'('</code> and closing <code>')'</code> parentheses
     * all key-value-pairs stored within the {@link Annotation} are emitted, separated by commas.
     * <p>
     * The key of a key-value-pair is returned, <i>as is</i>, while the value is formatted
     * using the {@link Object#toString()} method. If the value already is a {@link String},
     * quotes are inserted to mark its bounds.
     */
    public String formatAnnotation() {
        return annotation.getData().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(AnnotationView::formatEntry)
                .collect(Collectors.joining(",", "@" + annotation.getIdentifier() + "(", ")"));
    }

    protected static String formatEntry(Map.Entry<String, Object> entry) {
        return String.format("%s=%s", entry.getKey(), formatValue(entry.getValue()));
    }

    protected static String formatValue(Object value) {
        if (value instanceof String) {
            return String.format("\"%s\"", value);
        } else if (value instanceof Atom) {
            return value.toString();
        }
        return value.toString();
    }

    /**
     * This mixin can be used to mark a {@link AnnotationView} subclass as reversible.
     * <p>
     * This is, an {@link Annotation} with this view changes its meaning or values when
     * the annotated {@link Instruction Instruction}
     * is reversed.
     */
    public interface ReversibleView {
        Annotation reversed();
    }
}