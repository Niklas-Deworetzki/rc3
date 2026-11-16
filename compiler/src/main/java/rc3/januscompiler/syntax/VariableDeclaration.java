package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.types.Type;
import rc3.lib.parsing.Position;

/**
 * This class represents declared variables in the abstract syntax tree.
 * <p>
 * Syntax of {@link VariableDeclaration}:
 * <pre>
 *      VariableDeclaration ::= {@link Type} {@link Identifier}
 * </pre>
 * </p>
 */
public final class VariableDeclaration extends Declaration<VariableEntry> {
    /**
     * The {@link Type} bound to the name introduced by this declaration.
     */
    public final Type type;

    public VariableDeclaration(Position position, Identifier name, Type type) {
        super(position, name);
        this.type = type;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("VariableDeclaration",
                name,
                formatMember("type", String.valueOf(type)));
    }
}
