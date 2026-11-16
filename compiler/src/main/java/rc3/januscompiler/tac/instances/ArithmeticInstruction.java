package rc3.januscompiler.tac.instances;

import rc3.januscompiler.syntax.BinaryExpression;
import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * Performs an arithmetic operation on two {@link Operand Operands} storing the result into another {@link Operand}.
 * <p>
 * In ECO32 this instruction would translate to any of the arithmetic instructions, depending on the {@link Operator}.
 * </p>
 */
public class ArithmeticInstruction extends AssignmentInstruction {
    public final Operand lhs;
    public final Operand rhs;
    public final Operator operator;

    public ArithmeticInstruction(TemporaryOperand target, Operand lhs, Operand rhs, Operator operator) {
        super(target);
        this.lhs = lhs;
        this.rhs = rhs;
        this.operator = operator;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\t%s := %s %s %s", target, lhs, operator.getRepresentation(), rhs);
    }

    /**
     * This enumeration contains the supported arithmetic {@link Operator Operators}.
     */
    public enum Operator {
        // Representations are optimized for readability not compability with other languages.
        ADD("+"),
        SUB("-"),
        MUL("×"),
        DIV("/"),
        MOD("%"),
        AND("and"),
        OR("or"),
        XOR("xor");

        private final String representation;

        Operator(String representation) {
            this.representation = representation;
        }

        /**
         * Returns a textual representation for this {@link Operator}.
         */
        public String getRepresentation() {
            return representation;
        }

        /**
         * Converts a {@link BinaryExpression.BinaryOperator} into the equivalent {@link Operator}.
         */
        public static Operator getEquivalentOperator(BinaryExpression.BinaryOperator binaryOperator) {
            return switch (binaryOperator) {
                case ADD -> Operator.ADD;
                case SUB -> Operator.SUB;
                case MUL -> Operator.MUL;
                case DIV -> Operator.DIV;
                case MOD -> Operator.MOD;
                case AND -> Operator.AND;
                case OR -> Operator.OR;
                case XOR -> Operator.XOR;
                default -> null;
            };
        }
    }
}
