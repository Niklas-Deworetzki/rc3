package rc3.januscompiler.tac.table;

import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.types.Type;

import java.util.Objects;

/**
 * This class extends the information stored in {@link VariableEntry} by a {@link String}. This {@link String}
 * is the name the variable received by the {@link NameResolver} during code generation.
 */
public class Variable {
    public final String name;
    public final VariableEntry entry;

    public Variable(String name, VariableEntry entry) {
        this.name = name;
        this.entry = entry;
    }

    /**
     * Returns the name given to this variable by the {@link NameResolver} during
     * code generation.
     *
     * @return The name given by the {@link NameResolver}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link VariableEntry} of the variable, holding information from all previous analysis passes.
     *
     * @return The {@link VariableEntry} of the variable.
     */
    public VariableEntry getEntry() {
        return entry;
    }

    /**
     * @see VariableEntry#getType()
     */
    public Type getType() {
        return entry.getType();
    }

    /**
     * @see VariableEntry#isParameter()
     */
    public boolean isParameter() {
        return entry.isParameter();
    }

    /**
     * @see VariableEntry#getOffset()
     */
    public int getOffset() {
        return entry.getOffset();
    }

    @Override
    public int hashCode() {
        return entry.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof Variable) &&
                Objects.equals(((Variable) obj).entry, entry);
    }

    @Override
    public String toString() {
        return String.format("%s %s", entry.type, name);
    }
}
