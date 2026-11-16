package rc3.januscompiler.backend.tac.types;

import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;

import java.util.Map;

/**
 * This abstract class is used to represent types of the C programming language.
 */
public sealed abstract class CType permits CArrayType, CPointerType, CPrimitiveType {

    /**
     * Converts a Janus {@link Type} into the corresponding {@link CType}.
     * <p>
     * This conversion method has to account for the type of parameters, since in Janus parameters are passed
     * as by reference. This is translated into a pointer type in C.
     * </p>
     * <p>
     * The complete table of type translations:
     * <pre>
     *  |  Janus Type  |  Parameter  |   C Type  |
     *  |--------------|-------------|-----------|
     *  |     int      |     No      |    int    |
     *  |    stack     |     No      |   stack   |
     *  |    int [n]   |     No      |  int [n]  |
     *  |     int      |    Yes      |   int *   |
     *  |    stack     |    Yes      |  stack *  |
     *  |    int [n]   |    Yes      |   int *   |
     * </pre>
     * </p>
     *
     * @param janusType   The Janus type to translate.
     * @param isParameter <code>true</code> when the type is from a parameter.
     * @return The corresponding {@link CType}.
     */
    public static CType fromJanusType(Type janusType, boolean isParameter) {
        if (janusType instanceof PrimitiveType) {
            CType cType = Map.of(
                            PrimitiveType.IntType, CPrimitiveType.IntType,
                            PrimitiveType.StackType, CPrimitiveType.StackType)
                    .get(janusType);

            if (isParameter) { // Parameters are passed by reference.
                cType = new CPointerType(cType);
            }
            return cType;
        } else {
            ArrayType arrayType = (ArrayType) janusType;
            if (isParameter) { // Janus Arrays are a pointer to the first element.
                return new CPointerType(CPrimitiveType.IntType);
            } else { // But as a variable, the array has to be written, so the stack frame is large enough.
                return new CArrayType(CPrimitiveType.IntType, arrayType.size);
            }
        }
    }

    /**
     * Returns a name that can be used to identify the {@link CType} and reason about it. The name is also used to
     * automatically generate specialized functions depending on the type of a variable.
     *
     * @return The name of this {@link CType}.
     */
    public abstract String getName();

    /**
     * The default value for a member of this {@link CType}. This {@link String} is used as an initializer when
     * declaring variables of this {@link CType}.
     *
     * @return A {@link String} used as an initializer in C to create the default values for Janus variables.
     */
    public abstract String getInitialValue();

    /**
     * The prefix used to declare a variable of this {@link CType}.
     *
     * @return The prefix in a declaration for a variable of this {@link CType}.
     */
    protected abstract String declarationPrefix();

    /**
     * The suffix used to declare a variable of this {@link CType}.
     *
     * @return The suffix in a declaration for a variable of this {@link CType}.
     */
    protected abstract String declarationSuffix();

    /**
     * Creates a {@link String} that can be used to declare a variable of given name with this {@link CType} in C.
     * <p>
     * The declaration {@link String} returned by this function stitches together the {@link CType#declarationPrefix()},
     * the given variable name and the {@link CType#declarationSuffix()}.
     * </p>
     * <p>
     * Prefix and suffix {@link String Strings} are required since some {@link CType CTypes} (for example pointers)
     * require the pointer specification on the right-hand side of the variable name, while the element type
     * is specified on the left-hand side. The implementation of this method should preserve the precedences of
     * different type operators.
     * </p>
     *
     * @param variable The variable name used for the declaration.
     * @return A {@link String} that can be used to declare a variable in C.
     */
    public String createDeclaration(String variable) {
        return declarationPrefix() + variable + declarationSuffix();
    }

    @Override
    public String toString() {
        return createDeclaration("").trim();
    }
}
