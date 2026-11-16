package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;

import static rc3.lib.utils.SetOperations.concat;

/**
 * An instruction of the form
 * <pre>
 * x := M[y] := z
 * </pre>
 * where <var>x</var>, <var>y</var> and <var>z</var> are atoms. Read the value <var>v</var> of <var>z</var>.
 * If <var>z</var> is a variable, destroy it. Read the value <var>a</var> of <var>y</var> and the contents
 * <var>u</var> of the memory at address <var>a</var>. Store <var>v</var> at address <var>a</var>.
 * If <var>x</var> is a variable, create it and give it the value <var>u</var>.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class MemoryInterchangeInstruction extends Instruction {
    public Atom left;
    public MemoryAccess memoryAccess;
    public Atom right;

    public MemoryInterchangeInstruction(Atom left, MemoryAccess memoryAccess, Atom right) {
        this.left = left.copy();
        this.memoryAccess = memoryAccess.copy();
        this.right = right.copy();
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        left = left.replace(original, replacement);
        memoryAccess = memoryAccess.replace(original, replacement);
        right = right.replace(original, replacement);
    }

    @Override
    public MemoryInterchangeInstruction reverse() {
        return copyReversedAnnotations(
                new MemoryInterchangeInstruction(right.copy(), memoryAccess.copy(), left.copy()));
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<Variable> variablesCreated() {
        if (left instanceof Variable) {
            return List.of((Variable) left);
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesDestroyed() {
        if (right instanceof Variable) {
            return List.of((Variable) right);
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        return concat(left.findVariablesInAnnotations(), memoryAccess.variablesUsed(), right.findVariablesInAnnotations());
    }

    @Override
    public String getRepresentation() {
        return String.format("%s := %s := %s", left, memoryAccess, right);
    }
}
