package rc3.rssa.annotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * This class provides the definition of an {@link Annotation}.
 * <p>
 * It can be used to verify the state of an {@link Annotation} and its {@link AnnotationField fields},
 * decide whether an {@link Annotation} should be shown when producing a {@link String}
 * representation with it, and construct the {@link AnnotationView} for an {@link Annotation}.
 */
public record AnnotationHandle(Collection<AnnotationField> fields,
                               Function<Annotation, ? extends AnnotationView> viewConstructor,
                               boolean shouldHideFormat) {
    /**
     * Creates an {@link AnnotationHandle} with the given fields, a constructor for views and a boolean, deciding
     * whether the annotation should be hidden when producing a {@link String} representation.
     */
    public AnnotationHandle {
    }

    /**
     * Returns all {@link AnnotationField fields} specified for an {@link Annotation}.
     */
    public Collection<AnnotationField> getFields() {
        return fields;
    }

    /**
     * Constructs an {@link AnnotationView} and casts it into the requested return type.
     */
    @SuppressWarnings("unchecked")
    public <V extends AnnotationView> V constructView(Annotation annotation) {
        return (V) viewConstructor.apply(annotation);
    }

    /**
     * Returns <code>true</code> if an {@link Annotation} should <b>not</b> be included when
     * creating a {@link String} representation for an {@link Annotatable}.
     */
    @Override
    public boolean shouldHideFormat() {
        return shouldHideFormat;
    }

    @Override
    public String toString() {
        String modifier = (shouldHideFormat()) ? "hidden " : "";
        return String.format("%sAnnotationHandle%s", modifier, Arrays.toString(fields.toArray()));
    }

    /**
     * Creates a default {@link AnnotationHandle} that does not define any {@link AnnotationField fields},
     * creates a default {@link AnnotationView} in contrast to a specialized sub-class, and will include
     * the {@link Annotation} when creating a {@link String} representation.
     */
    public static AnnotationHandle defaultHandle() {
        return new AnnotationHandle(Collections.emptyList(), AnnotationView::new, false);
    }
}
