package rc3.rssa.instances;

import rc3.januscompiler.syntax.AssignStatement;
import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;

import static rc3.lib.utils.SetOperations.concat;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator;

/**
 * An instruction of the form
 * <pre>
 *  M[x] ⊕= y ⊙ z
 * </pre>
 * where <var>x</var>, <var>y</var> and <var>z</var> are atoms. The value <var>w</var> of <code>y ⊙ z</code>
 * is calculated, the value <var>a</var> of <var></var> is fetched and the contents <var>v</var> of memory
 * at address <var>a</var> is fetched. The contents of memory at address <var>a</var> is then given
 * the value <code>u = v ⊕ w</code>.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class MemoryAssignment extends Instruction {
    public MemoryAccess target;
    public AssignStatement.ModificationOperator operator;
    public Atom left;
    public BinaryOperator arithmeticOperator;
    public Atom right;

    public MemoryAssignment(MemoryAccess target, AssignStatement.ModificationOperator operator, Atom left, BinaryOperator arithmeticOperator, Atom right) {
        this.target = target.copy();
        this.operator = operator;
        this.left = left.copy();
        this.arithmeticOperator = arithmeticOperator;
        this.right = right.copy();
    }

    public MemoryAssignment(MemoryAccess target, AssignStatement.ModificationOperator operator, BinaryOperand value) {
        this(target, operator, (Atom) value.left, value.operator, (Atom) value.right);
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        target = target.replace(original, replacement);
        left = left.replace(original, replacement);
        right = right.replace(original, replacement);
    }

    @Override
    public MemoryAssignment reverse() {
        return copyReversedAnnotations(
                new MemoryAssignment(
                        target.copy(), operator.invert(),
                        left.copy(), arithmeticOperator, right.copy()));
    }

    /**
     * Returns the complete value, that is used to perform this assignment.
     *
     * <p>
     * In an assignment of the form <code>M[a] + (x * y)</code> the value returned by
     * this method is a {@link BinaryOperand} holding <code>(x + y)</code>.
     * </p>
     */
    public BinaryOperand getValue() {
        return new BinaryOperand(left, arithmeticOperator, right);
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
        return concat(
                target.variablesUsed(),
                left.variablesUsed(),
                right.variablesUsed());
    }

    @Override
    public String getRepresentation() {
        return String.format("%s %s= %s %s %s", target, operator, left, arithmeticOperator, right);
    }
}
