package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents {@link Variable Variables} that are used as an {@link Expression}
 * in the abstract syntax tree.
 * <p>
 * Syntax of {@link VariableExpression}:
 * <pre>
 *      VariableExpression ::= {@link Variable}
 * </pre>
 * </p>
 */
public final class VariableExpression extends Expression {
    /**
     * The {@link Variable} that is evaluated to its value.
     */
    public final Variable variable;

    public VariableExpression(Position position, Variable variable) {
        super(position);
        this.variable = variable;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("VariableExpression",
                formatMember("variable", String.valueOf(variable)));
    }
}
