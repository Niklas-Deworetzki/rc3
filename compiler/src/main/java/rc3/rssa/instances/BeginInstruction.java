package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.Set;

/**
 * An entry point
 * <pre>
 *  begin l(x, ...)
 * </pre>
 * where <var>l</var> is a label and the <var>x</var>s are atoms. This is a subroutine entry point.
 * If an <var>x</var> is a variable, it is created and given the value of the corresponding incoming argument.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class BeginInstruction extends ControlInstruction {
    public final String label;

    public BeginInstruction(String label) {
        super(true);
        this.label = label;
    }

    @Override
    public EndInstruction reverse() {
        EndInstruction result = new EndInstruction(label);
        result.parameters.addAll(this.copyParameters());
        return copyReversedAnnotations(result);
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Set<String> associatedLabels() {
        return Set.of(label);
    }

    @Override
    protected String getRepresentation() {
        return String.format("begin %s(%s)",
                label,
                formatParameterList());
    }
}
