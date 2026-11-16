package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.tac.types.CType;
import rc3.januscompiler.tac.table.Variable;

/**
 * This class represents variables in the C programming language.
 * <p>
 * In this implementation a variable is a name tied to a type. Opposite to how pointer types are handled in the
 * C programming language, they are considered part of the type and not the variable in this implementation.
 * </p>
 */
public record CVariable(String name, CType type) {

    /**
     * Creates a new {@link CVariable} from a Janus {@link Variable}.
     * <p>
     * This method maps the Janus types to the corresponding C types and also considers whether a variable
     * is a parameter in this translation.
     * </p>
     *
     * @param variable The {@link Variable} to be converted into a {@link CVariable}.
     * @return A {@link CVariable} for the {@link Variable}.
     */
    public static CVariable fromVariable(Variable variable) {
        CType type = CType.fromJanusType(variable.entry.type, variable.entry.isParameter);
        return new CVariable(variable.name, type);
    }

    /**
     * Returns a {@link String} that can be used in C to declare this {@link CVariable}.
     * <p>
     * This {@link String} representation combines the variable's name with a {@link String} representation
     * of the type.
     * </p>
     *
     * @return A {@link String} that declares this {@link CVariable} in C.
     */
    public String getDeclarationString() {
        return type.createDeclaration(this.name);
    }

    @Override
    public String toString() {
        return getDeclarationString();
    }
}
