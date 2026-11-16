package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents empty (skip) statements in the abstract syntax tree.
 * <p>
 * Syntax of {@link SkipStatement}:
 * <pre>
 *      SkipStatement ::= 'skip'
 * </pre>
 * </p>
 */
public final class SkipStatement extends Statement {
    public SkipStatement(Position position) {
        super(position);
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "SkipStatement()";
    }
}
