package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.lib.parsing.Position;

/**
 * This class represents array variables indexed by an integer in the abstract syntax tree.
 * <p>
 * Syntax of {@link IndexedVariable}:
 * <pre>
 *     IndexedVariable ::= {@link Identifier} '[' {@link Expression} ']'
 * </pre>
 * </p>
 */
public final class IndexedVariable extends Variable {
    /**
     * The index of the accessed array variable.
     */
    public final Expression index;

    public IndexedVariable(Position position, Identifier name, Expression index) {
        super(position, name);
        this.index = index;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("IndexedVariable",
                name,
                formatMember("index", String.valueOf(index)));
    }
}
