package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

/**
 * Represents a constant value in RSSA.
 */
public final class Constant extends Atom {
    public final int value;

    public Constant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Constant replace(Variable original, Atom replacement) {
        replaceVariablesInAnnotations(original, replacement);
        return this;
    }

    @Override
    public Atom copy() {
        return copyAnnotations(new Constant(value));
    }

    @Override
    protected String formatAtom() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Constant other) &&
                this.value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
