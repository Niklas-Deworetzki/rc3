package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} represents an conditional branch. Execution after this {@link Instruction} is
 * resumed at the given {@link Label} if the comparison within this {@link Instruction} evaluates to <code>true</code>.
 * <p>
 * In ECO32 this instruction would translate to one of the conditional branching instructions.
 * </p>
 *
 * @see RelativeOperator for supported comparison operators.
 */
public class IfGotoInstruction extends JumpingInstruction {
    public final Operand lhs;
    public final Operand rhs;
    public final RelativeOperator relativeOperator;

    public IfGotoInstruction(Operand lhs, Operand rhs, RelativeOperator relativeOperator, String label) {
        super(label);
        this.lhs = lhs;
        this.rhs = rhs;
        this.relativeOperator = relativeOperator;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\tIf %s %s %s Goto %s", lhs, relativeOperator.getRepresentation(), rhs, label);
    }

    /**
     * This enumeration holds the operators available to compare two {@link Operand Operands}.
     * <p>
     * The operator set is reduced to "less than" operators, since "greater than" operations can
     * be mapped onto the "less than" ones.
     * </p>
     */
    public enum RelativeOperator {
        EQU("="),
        NEQ("!="),
        LST("<"),
        LSE("<="),
        U_LST("u<"),
        U_LSE("u<=");

        private final String representation;

        RelativeOperator(String representation) {
            this.representation = representation;
        }

        public String getRepresentation() {
            return representation;
        }
    }
}
