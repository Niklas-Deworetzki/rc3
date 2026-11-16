package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.lib.parsing.Position;

/**
 * This class represents simple named variables in the abstract syntax tree.
 * <p>
 * Syntax of {@link NamedVariable}:
 * <pre>
 *      NamedVariable ::= {@link Identifier}
 * </pre>
 * </p>
 */
public final class NamedVariable extends Variable {
    public NamedVariable(Position position, Identifier name) {
        super(position, name);
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("NamedVariable", name);
    }
}
