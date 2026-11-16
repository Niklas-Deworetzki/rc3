package rc3.rssa.instances;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;
import java.util.Optional;

import static rc3.lib.utils.SetOperations.concat;

/**
 * Represents a value in memory.
 */
public final class MemoryAccess extends RValue {
    public final Atom index;

    public MemoryAccess(Atom index) {
        this.index = index.copy();
    }

    public Optional<Annotation> getBoundsCheck() {
        return findAnnotation("InBounds");
    }


    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public MemoryAccess replace(Variable original, Atom replacement) {
        MemoryAccess copy = new MemoryAccess(index.replace(original, replacement));
        for (Annotation annotation : getAnnotations()) {
            copy.annotate(annotation.replaceVariable(original, replacement));
        }
        return copy;
    }

    @Override
    public MemoryAccess copy() {
        MemoryAccess copy = new MemoryAccess(index.copy());
        for (Annotation annotation : getAnnotations()) {
            copy.annotate(annotation.copy());
        }
        return copy;
    }

    @Override
    public List<Variable> variablesUsed() {
        return concat(index.variablesUsed(),
                findVariablesInAnnotations());
    }

    @Override
    public String toString() {
        return String.format("M[%s%s]", index, formatAnnotations());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryAccess && ((MemoryAccess) obj).index.equals(index);
    }

    @Override
    public int hashCode() {
        return index.hashCode();
    }
}
