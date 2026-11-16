package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.Set;

/**
 * An entry point
 * <pre>
 *  l(x, ...) <-
 * </pre>
 * where <var>l</var> is a label. This is an unconditional entry point.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class UnconditionalEntry extends ControlInstruction implements ControlInstruction.Unconditional {
    public String label;

    public UnconditionalEntry(String label) {
        super(true);
        this.label = label;
    }

    @Override
    public UnconditionalExit reverse() {
        UnconditionalExit result = new UnconditionalExit(label);
        result.parameters.addAll(this.copyParameters());
        return copyReversedAnnotations(result);
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public Set<String> associatedLabels() {
        return Set.of(label);
    }

    @Override
    public String getRepresentation() {
        return String.format("%s(%s) <-",
                label,
                formatParameterList());
    }
}
