package rc3.januscompiler.table;

import rc3.januscompiler.backend.VariableAllocator;
import rc3.januscompiler.syntax.VariableDeclaration;
import rc3.januscompiler.types.Type;

/**
 * This class holds the information about declared variables.
 *
 * @see VariableDeclaration
 */
public final class VariableEntry extends Entry {
    /**
     * The {@link Type} used to declare the variable.
     */
    public final Type type;
    /**
     * Whether or not the variable is a parameter.
     */
    public final boolean isParameter;
    /**
     * The memory offset within the stack frame for this variable.
     *
     * @implNote This information is filled in by a {@link VariableAllocator}.
     */
    public int offset;

    public VariableEntry(Identifier name, Type type, boolean isParameter) {
        super(name);
        this.type = type;
        this.isParameter = isParameter;
    }

    /**
     * Returns the {@link Type} used to declare this variable.
     *
     * @see VariableEntry#type
     */
    public Type getType() {
        return type;
    }

    /**
     * Checks whether or not this variable is declared as a parameter.
     *
     * @return <code>true</code> if this variable is a parameter.
     * @see VariableEntry#isParameter
     */
    public boolean isParameter() {
        return isParameter;
    }

    /**
     * Returns the memory offset within a stack frame for this variable.
     *
     * @apiNote The value returned has to be initialized by a {@link VariableAllocator}.
     * @see VariableEntry#offset
     */
    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("VariableEntry{name=%s, type=%s, isParameter=%s, offset=%s}",
                name, type, isParameter, offset);
    }
}
