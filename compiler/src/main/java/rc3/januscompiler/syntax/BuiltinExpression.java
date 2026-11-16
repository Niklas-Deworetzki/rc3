package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Builtins;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents available {@link Expression Expressions} that are builtin into Janus.
 *
 * @see Builtins.Expression for available {@link BuiltinExpression BuiltinExpressions}.
 */
public final class BuiltinExpression extends Expression {
    /**
     * The builtin expression represented by this instance.
     */
    public final Builtins.Expression expression;
    /**
     * A {@link List} of arguments supplied to this {@link Expression}
     */
    public final List<Variable> argumentList;

    public BuiltinExpression(Position position, Builtins.Expression expression, List<Variable> argumentList) {
        super(position);
        this.expression = expression;
        this.argumentList = argumentList;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
