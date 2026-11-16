package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents the literal representing the empty stack in the abstract syntax tree.
 * <p>
 * Syntax of {@link StackNilLiteral}:
 * <pre>
 *      StackNilLiteral ::= 'nil'
 * </pre>
 * </p>
 */
public final class StackNilLiteral extends Expression {
    public StackNilLiteral(Position position) {
        super(position);
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "nil";
    }
}
