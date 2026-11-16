package rc3.januscompiler.table;

import rc3.januscompiler.backend.VariableAllocator;
import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.types.Type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class holds the information about declared procedures.
 *
 * @see ProcedureDeclaration
 */
public final class ProcedureEntry extends Entry {
    /**
     * The formal parameters specified for this procedure.
     */
    public final List<ParameterType> parameterTypes;
    /**
     * The memory required for local variables in this procedure.
     *
     * @implNote This variable is set by a {@link VariableAllocator}.
     */
    public int localVariableAreaSize;
    /**
     * The memory required to store parameters.
     *
     * @implNote This variable is set by a {@link VariableAllocator}.
     */
    public int parameterAreaSize;
    /**
     * The memory required to store arguments to a called procedure.
     *
     * @implNote This variable is set by a {@link VariableAllocator}.
     */
    public int argumentAreaSize;

    /**
     * This constant is used if no other procedure is called by this procedure, since this enables
     * certain optimizations.
     *
     * @see ProcedureEntry#callsOtherProcedures()
     */
    public static final int NO_PROCEDURES_CALLED = -1;

    public ProcedureEntry(Identifier name, List<Type> parameterTypes) {
        super(name);
        this.parameterTypes = convertParameterTypes(parameterTypes);
    }

    /**
     * Converts a {@link List} of {@link Type Types} into a {@link List} of {@link ParameterType ParameterTypes}.
     *
     * @param parameterTypes A {@link List} holding the {@link Type Types} of parameters.
     * @return A {@link List} of {@link ParameterType ParameterTypes}.
     */
    private static List<ParameterType> convertParameterTypes(List<Type> parameterTypes) {
        return parameterTypes.stream()
                .map(ParameterType::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the {@link List} of {@link ParameterType ParameterTypes} defined for this procedure.
     *
     * @return The procedures parameter list.
     */
    public List<ParameterType> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Returns the memory required for local variables.
     *
     * @apiNote The value returned has to be initialized by a {@link VariableAllocator}.
     * @see ProcedureEntry#localVariableAreaSize
     */
    public int getLocalVariableAreaSize() {
        return localVariableAreaSize;
    }

    /**
     * Returns the memory required to store parameters.
     *
     * @apiNote The value returned has to be initialized by a {@link VariableAllocator}.
     * @see ProcedureEntry#parameterAreaSize
     */
    public int getParameterAreaSize() {
        return parameterAreaSize;
    }

    /**
     * Returns the memory required to store arguments passed to another procedure.
     *
     * @apiNote The value returned has to be initialized by a {@link VariableAllocator}.
     * @see ProcedureEntry#argumentAreaSize
     */
    public int getArgumentAreaSize() {
        return argumentAreaSize;
    }

    /**
     * Checks whether or not this procedure calls other procedures.
     *
     * @return <code>true</code> if another procedure is called.
     * @apiNote The value returned has to be initialized by a {@link VariableAllocator}.
     */
    public boolean callsOtherProcedures() {
        return argumentAreaSize != NO_PROCEDURES_CALLED;
    }

    @Override
    public String toString() {
        return String.format(
                "ProcedureEntry{name=%s, parameterTypes=%s, localVariableAreaSize=%s, parameterAreaSize=%s, argumentAreaSize=%s}",
                name, parameterTypes, localVariableAreaSize, parameterAreaSize, argumentAreaSize);
    }
}
