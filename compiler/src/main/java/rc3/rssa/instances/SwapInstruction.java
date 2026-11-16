package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;

import static rc3.lib.utils.SetOperations.concat;

/**
 * Represents an instruction of the form
 * <pre>
 *   x, y := z, w
 * </pre>
 * The variables <var>z</var> and <var>w</var> are destroyed and their values given
 * to the variables <var>x</var> and <var>y</var>, which are created.
 * <p>
 *
 * @implNote While in the description of this instruction seems to explicitly state <i>variables</i> as
 * operands, in RSSA every variable is replaceable by a constant. Therefore <i>atoms</i> are the operands
 * allowed for this instruction.
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class SwapInstruction extends Instruction {
    public Atom targetLeft;
    public Atom targetRight;
    public Atom sourceLeft;
    public Atom sourceRight;

    public SwapInstruction(Atom targetLeft, Atom targetRight, Atom sourceLeft, Atom sourceRight) {
        this.targetLeft = targetLeft.copy();
        this.targetRight = targetRight.copy();
        this.sourceLeft = sourceLeft.copy();
        this.sourceRight = sourceRight.copy();
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        targetLeft = targetLeft.replace(original, replacement);
        targetRight = targetRight.replace(original, replacement);
        sourceLeft = sourceLeft.replace(original, replacement);
        sourceRight = sourceRight.replace(original, replacement);
    }

    @Override
    public SwapInstruction reverse() {
        return copyReversedAnnotations(
                new SwapInstruction(
                        sourceLeft.copy(),
                        sourceRight.copy(),
                        targetLeft.copy(),
                        targetRight.copy()));
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<Variable> variablesCreated() {
        return atomsAsVariableList(targetLeft, targetRight);
    }

    @Override
    public List<Variable> variablesDestroyed() {
        return atomsAsVariableList(sourceLeft, sourceRight);
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        return concat(targetLeft.findVariablesInAnnotations(), targetRight.findVariablesInAnnotations(),
                sourceLeft.findVariablesInAnnotations(), sourceRight.findVariablesInAnnotations());
    }

    @Override
    public String getRepresentation() {
        return String.format("%s, %s := %s, %s", targetLeft, targetRight, sourceLeft, sourceRight);
    }
}
