package rc3.januscompiler.backend.tac.types;

import java.util.Objects;

/**
 * This class represents pointer types in the C programming language.
 * <p>
 * Even if it is not necessarily required at the current state, this class can handle multiply nested pointer instances.
 * </p>
 */
public final class CPointerType extends CType {
    public final CType elementType;

    public CPointerType(CType elementType) {
        this.elementType = elementType;
    }

    @Override
    public String getName() {
        return elementType.getName() + "Pointer";
    }

    @Override
    public String getInitialValue() {
        return "NULL";
    }

    // Required to preserve the precedence of type operators.
    private boolean declarationRequiresParenthesis() {
        return elementType instanceof CArrayType;
    }

    @Override
    protected String declarationPrefix() {
        String result = elementType.declarationPrefix();
        if (declarationRequiresParenthesis()) {
            result += "(";
        }
        return result + "*";
    }

    @Override
    protected String declarationSuffix() {
        String result = elementType.declarationSuffix();
        if (declarationRequiresParenthesis()) {
            result = ")" + result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof CPointerType &&
                Objects.equals(elementType, ((CPointerType) obj).elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }
}
