package rc3.januscompiler.syntax;

import rc3.januscompiler.table.Entry;
import rc3.januscompiler.table.Identifier;
import rc3.lib.parsing.Position;

/**
 * This abstract superclass represents all declarations.
 * <p>
 * Declarations store the information associated with them in an {@link Entry} field.
 * The type of that field can be specialized in the concrete subclasses.
 * </p>
 *
 * @param <E> The type of the {@link Declaration#entry} field holding the information of the declaration.
 */
public abstract sealed class Declaration<E extends Entry> extends Tree
        permits ProcedureDeclaration, VariableDeclaration {
    /**
     * The name introduced with this {@link Declaration}.
     */
    public final Identifier name;
    /**
     * The {@link Entry} holding the information associated with this declaration.
     *
     * @apiNote This information is populated during global and local semantic analysis.
     */
    public E entry;

    public Declaration(Position position, Identifier name) {
        super(position);
        this.name = name;
    }
}
