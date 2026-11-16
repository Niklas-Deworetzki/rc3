package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;

import static rc3.lib.utils.SetOperations.concat;

/**
 * An instruction of the form
 * <pre>
 * M[x] <-> M[y]
 * </pre>
 * where <var>x</var> and <var>y</var> are atoms. The values <var>a</var> and <var>b</var> of <var>x</var>
 * and <var>y</var> are fetched, and the contents of memory at addresses <var>a</var> and <var>b</var> are
 * interchanged.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class MemorySwapInstruction extends Instruction {
    public MemoryAccess left;
    public MemoryAccess right;

    public MemorySwapInstruction(MemoryAccess left, MemoryAccess right) {
        this.left = left.copy();
        this.right = right.copy();
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        left = left.replace(original, replacement);
        right = right.replace(original, replacement);
    }

    @Override
    public MemorySwapInstruction reverse() {
        return copyReversedAnnotations(
                new MemorySwapInstruction(right.copy(), left.copy()));
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<Variable> variablesCreated() {
        return List.of();
    }

    @Override
    public List<Variable> variablesDestroyed() {
        return List.of();
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        return concat(left.variablesUsed(), right.variablesUsed());
    }

    @Override
    public String getRepresentation() {
        return String.format("%s <-> %s", left, right);
    }
}
