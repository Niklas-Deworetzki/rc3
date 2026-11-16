package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.Set;

/**
 * An exit point
 * <pre>
 *  end l(y, ...)
 * </pre>
 * where <var>l</var> is a label and the <var>y</var>s are atoms. This is a subroutine exit point.
 * The values of <code>(y, ...)</code> are passed back to the caller.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class EndInstruction extends ControlInstruction {
    public final String label;

    public EndInstruction(String label) {
        super(false);
        this.label = label;
    }

    @Override
    public BeginInstruction reverse() {
        BeginInstruction result = new BeginInstruction(label);
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
    public String getRepresentation() {
        return String.format("end %s(%s)",
                label,
                formatParameterList());
    }
}
