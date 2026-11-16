package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents assign statements in the abstract syntax tree.
 * <p>
 * Syntax of {@link AssignStatement}:
 * <pre>
 *     AssignStatement ::= {@link Variable} ModificationOperator {@link Expression}
 *
 *     ModificationOperator ::= '+=' | '-=' | '^='
 * </pre>
 * </p>
 *
 * @see ModificationOperator for possible operators in assign statements.
 */
public final class AssignStatement extends Statement {
    /**
     * The target of the assignment. This {@link Variable} is modified.
     */
    public final Variable variable;
    /**
     * The operator used to modify the {@link AssignStatement#variable}.
     */
    public final ModificationOperator modificationOperator;
    /**
     * The value the {@link AssignStatement#variable} is modified by.
     */
    public final Expression value;

    public AssignStatement(Position position, Variable variable,
                           ModificationOperator modificationOperator, Expression value) {
        super(position);
        this.variable = variable;
        this.modificationOperator = modificationOperator;
        this.value = value;
    }

    /**
     * An enumeration of reversible assignment operators in Janus.
     */
    public enum ModificationOperator {
        ADD("+"), SUB("-"), XOR("^");

        private final String representation;

        ModificationOperator(String representation) {
            this.representation = representation;
        }

        @Override
        public String toString() {
            return representation;
        }

        public ModificationOperator invert() {
            return switch (this) {
                case ADD -> SUB;
                case SUB -> ADD;
                case XOR -> XOR;
            };
        }

        public int evaluateConstant(int left, int right) {
            return switch (this) {
                case XOR -> left ^ right;
                case ADD -> left + right;
                case SUB -> left - right;
            };
        }
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("AssignStatement",
                formatMember("operator", modificationOperator.name()),
                formatMember("variable", String.valueOf(variable)),
                formatMember("value", String.valueOf(value)));
    }
}
