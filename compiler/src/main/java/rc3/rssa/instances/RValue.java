package rc3.rssa.instances;

import rc3.rssa.annotations.Annotatable;
import rc3.rssa.annotations.Annotation;

import java.util.HashSet;
import java.util.Set;

/**
 * A R-Value <var>R</var> is either an atom or of the form <var>M[a]</var>, where <var>a</var> is an atom.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 211. In: Perspectives of System Informatics 2015
 */
public sealed abstract class RValue implements Annotatable, Value permits Atom, MemoryAccess {
    private final Set<Annotation> annotations = new HashSet<>(0 /* We rarely use any Annotations */);


    @Override
    public final Set<Annotation> getAnnotations() {
        return annotations;
    }


    @Override
    public abstract RValue replace(Variable original, Atom replacement);

    @Override
    public abstract RValue copy();
}
