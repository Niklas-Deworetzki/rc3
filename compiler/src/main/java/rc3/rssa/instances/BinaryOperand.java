package rc3.rssa.instances;

import rc3.januscompiler.syntax.AssignStatement;
import rc3.rssa.visitor.RSSAVisitor;

import java.util.*;

import static rc3.lib.utils.SetOperations.concat;

/**
 * Represents a combination of two {@link RValue RValues} with a binary operator.
 *
 * @implNote This class is not explicitly used as an operand for other instructions. It is used as the
 * returned value when generating code for expressions. Using this class it is possible for an expression
 * to evaluate into a single object which simplifies code generation for expressions. For use in instructions
 * instances of this class have to be unpacked first.
 */
public final class BinaryOperand implements Value {
    public final RValue left;
    public final BinaryOperator operator;
    public final RValue right;

    public BinaryOperand(RValue left, BinaryOperator operator, RValue right) {
        this.left = left.copy();
        this.operator = operator;
        this.right = right.copy();
    }

    /**
     * Creates a no-op {@link BinaryOperand} that evaluates to the given {@link RValue}.
     */
    public static BinaryOperand of(RValue value) {
        return new BinaryOperand(value, BinaryOperator.XOR, new Constant(0));
    }

    @Override
    public <T> T accept(RSSAVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public BinaryOperand replace(Variable original, Atom replacement) {
        return new BinaryOperand(
                left.replace(original, replacement),
                operator,
                right.replace(original, replacement));
    }

    @Override
    public BinaryOperand copy() {
        return new BinaryOperand(left.copy(), operator, right.copy());
    }

    @Override
    public List<Variable> variablesUsed() {
        return concat(left.variablesUsed(), right.variablesUsed());
    }

    @Override
    public String toString() {
        return String.join(" ", left.toString(), operator.toString(), right.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BinaryOperand other)) return false;

        return this.left.equals(other.left) &&
                this.right.equals(other.right) &&
                this.operator == other.operator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, operator);
    }

    /**
     * Evaluates this {@link BinaryOperand} to an <code>int</code> value, if possible.
     *
     * @see BinaryOperand#evaluateConstant(RValue, BinaryOperator, RValue)
     */
    public OptionalInt evaluateConstant() {
        return evaluateConstant(left, operator, right);
    }

    /**
     * Evaluates a binary operation into a constant <code>int</code> value, if possible.
     * <p>
     * The {@link BinaryOperator} must be an {@link BinaryOperator#isArithmetic() arithmetic operator},
     * to evaluate it. Evaluation is possible in the following cases:
     * <ul>
     *  <li>If both operands are {@link Constant}, the arithmetic result is computed.</li>
     *  <li>If one of the operands is {@link Constant} 0, the result will be 0 as well.</li>
     *  <li>If both operands are identical and the operator is {@link BinaryOperator#MOD} or
     *  {@link BinaryOperator#SUB}, the result will be 0.</li>
     *  <li>If both operands are identical and the operator is {@link BinaryOperator#DIV},
     *  the result will be 0.</li>
     * </ul>
     * <p>
     * If during constant evaluation, a mathematical error occurs (such as division by 0),
     * an empty result will be returned.
     */
    public static OptionalInt evaluateConstant(RValue lhs, BinaryOperator operator, RValue rhs) {
        if (lhs instanceof Constant lConst && rhs instanceof Constant rConst) {
            return operator.evaluateConstant(lConst.getValue(), rConst.getValue());

        } else if (operator == BinaryOperator.MUL) {
            if (lhs instanceof Constant lConst && lConst.getValue() == 0)
                return OptionalInt.of(0);
            if (rhs instanceof Constant rConst && rConst.getValue() == 0)
                return OptionalInt.of(0);

        } else if (lhs.equals(rhs)) {
            switch (operator) {
                case SUB, MOD:
                    return OptionalInt.of(0);
                case DIV:
                    return OptionalInt.of(1);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Evaluates this {@link BinaryOperand} into a constant <code>boolean</code>, if possible.
     *
     * @see BinaryOperand#compareConstant(RValue, BinaryOperator, RValue)
     */
    public Optional<Boolean> compareConstant() {
        return compareConstant(left, operator, right);
    }

    /**
     * Evaluates a binary comparator into a constant <code>boolean</code> value, if possible.
     * <p>
     * The following cases are recognized by this function:
     * <ul>
     *  <li>If both operands are a {@link Constant}, the comparison operator is applied to them.</li>
     *  <li>If both operands are the same and the comparison operator is reflexive, <code>true</code>
     *  is returned.</li>
     *  <li>If both operands are the same and the comparison operator is anti-reflexive,
     *  <code>false</code> is returned.</li>
     * </ul>
     */
    public static Optional<Boolean> compareConstant(RValue lhs, BinaryOperator operator, RValue rhs) {
        if (operator.isComparison() && lhs instanceof Constant lConst && rhs instanceof Constant rConst) {
            return Optional.of(operator.compareConstant(lConst.getValue(), rConst.getValue()));

        } else if (lhs.equals(rhs)) {
            return switch (operator) {
                case LSE, GRE, EQU -> Optional.of(true);
                case LST, GRT, NEQ -> Optional.of(false);
                default -> Optional.empty();
            };

        } else {
            return Optional.empty();
        }
    }

    /**
     * An enumeration of all supported binary operators.
     *
     * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 207. In: Perspectives of System Informatics 2015
     */
    public enum BinaryOperator {
        // +, -, ^ , &, |, >>, or << from RIL (Mogensen, Garbage Collection for Reversible Functional Languages).
        ADD("+", 0),
        SUB("-", 0),
        XOR("^", 0),
        OR("|", 0),
        SAL("<<", 0),
        SAR(">>", 0),

        // ==, <, >, !=, <=, >= and & using notation from the programming language C.
        EQU("=="),
        LST("<"),
        GRT(">"),
        NEQ("!=", 0),
        LSE("<="),
        GRE(">="),

        AND("&", -1),

        // Added to allow translation from Janus. MUL, DIV and MOD are not present in original RSSA.
        MUL("*", 1),
        DIV("/", 1),
        MOD("%", 1);

        private final String representation;

        private final boolean hasIdentity;
        private final int identity;

        BinaryOperator(String representation) {
            this.representation = representation;
            this.identity = 0xAAAA;
            this.hasIdentity = false;
        }

        BinaryOperator(String representation, int identity) {
            this.representation = representation;
            this.identity = identity;
            this.hasIdentity = true;
        }

        @Override
        public String toString() {
            return representation;
        }

        public boolean isIdentity(Constant constant) {
            return isIdentity(constant.value);
        }

        public boolean isIdentity(int constant) {
            return hasIdentity && constant == identity;
        }

        public int getIdentity() {
            if (!hasIdentity) {
                throw new NoSuchElementException("No arithmetic identity for operator " + this);
            }
            return identity;
        }

        public OptionalInt evaluateConstant(int left, int right) {
            switch (this) {
                case ADD:
                    return OptionalInt.of(left + right);
                case SUB:
                    return OptionalInt.of(left - right);
                case XOR:
                    return OptionalInt.of(left ^ right);
                case OR:
                    return OptionalInt.of(left | right);
                case SAL:
                    return OptionalInt.of(left << right);
                case SAR:
                    return OptionalInt.of(left >> right);
                case AND:
                    return OptionalInt.of(left & right);
                case MUL:
                    return OptionalInt.of(left * right);
                case DIV:
                    if (right != 0) {
                        return OptionalInt.of(left / right);
                    }
                    return OptionalInt.empty();
                case MOD:
                    if (right != 0) {
                        return OptionalInt.of(left % right);
                    }
                    return OptionalInt.empty();
            }
            return OptionalInt.empty();
        }

        public boolean compareConstant(int lhs, int rhs) throws IllegalStateException {
            return switch (this) {
                case EQU -> lhs == rhs;
                case NEQ -> lhs != rhs;
                case LST -> lhs < rhs;
                case LSE -> lhs <= rhs;
                case GRT -> lhs > rhs;
                case GRE -> lhs >= rhs;
                default -> throw new IllegalStateException("Operator " + this + " is not a comparison operator!");
            };
        }

        public boolean isArithmetic() {
            return switch (this) {
                case ADD, SUB, XOR, OR, SAL, SAR, AND, MOD, MUL, DIV -> true;
                default -> false;
            };
        }

        public boolean isComparison() {
            return switch (this) {
                case EQU, NEQ, LST, LSE, GRT, GRE -> true;
                default -> false;
            };
        }

        public boolean isValidInCondition() {
            return switch (this) {
                case EQU, LST, GRT, NEQ, LSE, GRE, AND, OR -> true;
                default -> false;
            };
        }

        public boolean isCommutative() {
            return switch (this) {
                case ADD, XOR, AND, OR, MUL -> true;
                default -> false;
            };
        }

        public boolean isAssignmentOperator() {
            return switch (this) {
                case ADD, SUB, XOR -> true;
                default -> false;
            };
        }

        public Optional<AssignStatement.ModificationOperator> getAssignmentVariant() {
            return switch (this) {
                case ADD -> Optional.of(AssignStatement.ModificationOperator.ADD);
                case SUB -> Optional.of(AssignStatement.ModificationOperator.SUB);
                case XOR -> Optional.of(AssignStatement.ModificationOperator.XOR);
                default -> Optional.empty();
            };
        }
    }
}
