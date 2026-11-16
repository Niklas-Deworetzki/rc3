package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents integer literals in the abstract syntax tree.
 */
public final class IntLiteral extends Expression {
    public final int value;

    public IntLiteral(Position position, int value) {
        super(position);
        this.value = value;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
