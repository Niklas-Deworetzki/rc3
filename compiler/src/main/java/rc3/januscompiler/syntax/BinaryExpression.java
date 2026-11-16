package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

import static rc3.januscompiler.syntax.BinaryExpression.OperatorType.*;

/**
 * This class represents binary expressions in the abstract syntax tree.
 * <p>
 * Syntax of {@link BinaryExpression}:
 * <pre>
 *      BinaryExpression ::= {@link Expression} BinaryOperator {@link Expression}
 *
 *      BinaryOperator ::= '+' | '-' | '*' | '/' | '%' | '&' | '|' | '^' | '&&' | '||'
 *                       | '<' | '<=' | '>' | '=>' | '=' | '!='
 * </pre>
 * </p>
 *
 * @see BinaryOperator for possible binary operators.
 */
public final class BinaryExpression extends Expression {
    /**
     * A sub expressions of this {@link BinaryExpression}.
     */
    public final Expression lhs, rhs;
    /**
     * The operator combining the two sub expressions.
     */
    public final BinaryOperator operator;

    public BinaryExpression(Position position, Expression lhs, BinaryOperator operator, Expression rhs) {
        super(position);
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    /**
     * An enumeration of different operator kinds available in Janus.
     * Every kind of operators has different properties.
     */
    public enum OperatorType {
        /**
         * Combines two integers to another integer using arithmetic.
         */
        ARITHMETIC,
        /**
         * Combines two integers to another integer using bit manipulation.
         */
        BITWISE,
        /**
         * Combines two booleans to another boolean.
         */
        LOGIC,
        /**
         * Combines two integers to a boolean value.
         */
        RELATIONAL
    }

    /**
     * An enumeration of binary operators available in Janus.
     *
     * @see OperatorType for different kinds of operators.
     */
    public enum BinaryOperator {
        ADD("+", ARITHMETIC),
        SUB("-", ARITHMETIC),
        MUL("*", ARITHMETIC),
        DIV("/", ARITHMETIC),
        MOD("%", ARITHMETIC),

        AND("&", BITWISE),
        OR("|", BITWISE),
        XOR("^", BITWISE),

        CONJ("&&", LOGIC),
        DISJ("||", LOGIC),

        LST("<", RELATIONAL),
        LSE("<=", RELATIONAL),
        GRT(">", RELATIONAL),
        GRE(">=", RELATIONAL),
        EQU("=", RELATIONAL),
        NEQ("!=", RELATIONAL);

        private final String representation;
        private final OperatorType type;

        BinaryOperator(String representation, OperatorType type) {
            this.representation = representation;
            this.type = type;
        }

        public OperatorType getType() {
            return type;
        }


        @Override
        public String toString() {
            return representation;
        }
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("BinaryExpression",
                formatMember("operator", operator.name()),
                formatMember("left operand", String.valueOf(lhs)),
                formatMember("right operand", String.valueOf(rhs)));
    }
}
