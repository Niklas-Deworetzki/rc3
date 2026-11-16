package rc3.rssa.annotations;

import rc3.lib.parsing.Position;
import rc3.rssa.annotations.views.SourceView;
import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This interface marks RSSA classes, whose instances can have {@link Annotation Annotations}.
 */
public interface Annotatable {

    /**
     * Returns all {@link Annotation Annotations} added to this instance.
     */
    Set<Annotation> getAnnotations();

    /**
     * Removes all {@link Annotation Annotations} associated with this instance.
     */
    default void clearAnnotations() {
        getAnnotations().clear();
    }

    /**
     * Adds the given {@link Annotation} to this instance.
     * <p>
     * If this instance is already annotated with an {@link Annotation} of the same class as the class
     * of the {@link Annotation} passed as an parameter, this {@link Annotation} is removed before adding
     * the parameter.
     * </p>
     *
     * @param annotation An {@link Annotation} to add to this instance. Other {@link Annotation Annotations} with
     *                   the same identifier are overwritten.
     */
    default void annotate(Annotation annotation) {
        Optional<Annotation> annotationPresent = findAnnotation(annotation.getIdentifier());

        annotationPresent.ifPresent(getAnnotations()::remove);
        getAnnotations().add(annotation);
    }

    /**
     * Returns a {@link List} of all {@link Variable Variables} that are part of
     * {@link Annotation Annotations} present on this instance.
     */
    default List<Variable> findVariablesInAnnotations() {
        return getAnnotations().stream()
                .flatMap(annotation -> annotation.variablesUsed().stream())
                .collect(Collectors.toList());
    }

    /**
     * Mutates this instance, replacing the {@link Variable Variables} in all
     * {@link Annotation Annotations} present on this instance.
     *
     * @see Annotation#replaceVariable(Variable, Atom)
     */
    default void replaceVariablesInAnnotations(Variable original, Atom replacement) {
        List<Annotation> replacedAnnotations = getAnnotations().stream()
                .map(annotation -> annotation.replaceVariable(original, replacement))
                .collect(Collectors.toList());

        annotate(replacedAnnotations);
    }

    /**
     * Adds the given {@link Collection} of {@link Annotation Annotations} to this instance.
     *
     * @param annotations A {@link Collection} of {@link Annotation Annotations} to add to this instance.
     * @see #annotate(Annotation)
     */
    default void annotate(Collection<Annotation> annotations) {
        annotations.forEach(this::annotate);
    }

    /**
     * Finds an {@link Annotation} of the given class.
     */
    default Optional<Annotation> findAnnotation(String identifier) {
        return getAnnotations().stream()
                .filter(annotation -> annotation.hasIdentifier(identifier))
                .findAny();
    }

    /**
     * Returns a {@link String} representation of all annotated {@link Annotation Annotations}.
     */
    default String formatAnnotations() {
        StringBuilder sb = new StringBuilder();
        for (Annotation annotation : getAnnotations()) {
            if (!annotation.shouldHideFormat()) {
                sb.append(' ').append(annotation);
            }
        }

        return sb.toString();
    }

    default Optional<Position> getSourcePosition() {
        return this.findAnnotation("Source")
                .map(annotation -> ((SourceView) annotation.getView()).getPosition());
    }
}
