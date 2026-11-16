package rc3.rssa.instances;

import rc3.rssa.annotations.Annotation;

import java.util.List;

import static rc3.lib.utils.SetOperations.concat;

/**
 * An atom <var>x</var> is either a variable or a constant.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 211. In: Perspectives of System Informatics 2015
 */
public sealed abstract class Atom extends RValue permits Constant, Variable {

    @Override
    public abstract Atom replace(Variable original, Atom replacement);

    @Override
    public abstract Atom copy();

    @Override
    public final List<Variable> variablesUsed() {
        if (this instanceof Variable variable) {
            return concat(List.of(variable), findVariablesInAnnotations());
        } else {
            return findVariablesInAnnotations();
        }
    }

    protected <A extends Atom> A copyAnnotations(A atom) {
        for (Annotation annotation : getAnnotations()) {
            atom.annotate(annotation.copy());
        }
        return atom;
    }

    protected abstract String formatAtom();

    @Override
    public final String toString() {
        if (getAnnotations().isEmpty()) {
            return formatAtom();
        } else {
            return String.format("%s %s", formatAtom(), formatAnnotations());
        }
    }
}
