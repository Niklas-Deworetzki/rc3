package rc3.januscompiler.table;

/**
 * The abstract superclass of all entries.
 * <p>
 * This class contains the information bound to a name by a declaration. This abstract superclass
 * only holds the {@link Identifier} used in the declaration.
 * </p>
 *
 * @see ProcedureEntry
 * @see VariableEntry
 */
public sealed abstract class Entry permits ProcedureEntry, VariableEntry {
    protected final Identifier name;

    public Entry(Identifier name) {
        this.name = name;
    }

    /**
     * The {@link Identifier} used in the declaration that created this {@link Entry}.
     *
     * @return The {@link Identifier} used in the declaration.
     */
    public Identifier getName() {
        return name;
    }

    /**
     * The {@link String} representation of the {@link Identifier} used in the declaration
     * that created this {@link Entry}.
     *
     * @return The {@link String} representation of {@link Entry#getName()}.
     */
    public String getStringName() {
        return name.name;
    }
}
