package rc3.januscompiler.backend.tac.types;

import java.util.Objects;

/**
 * This class represents array types in the C programming language.
 */
public final class CArrayType extends CType {
    public final CType elementType;
    public final int size;

    public CArrayType(CType elementType, int size) {
        this.elementType = elementType;
        this.size = size;
    }

    @Override
    public String getName() {
        return elementType.getName() + "Array";
    }

    @Override
    public String getInitialValue() {
        return "{ 0 }";
    }

    @Override
    protected String declarationPrefix() {
        return elementType.declarationPrefix();
    }

    @Override
    protected String declarationSuffix() {
        return elementType.declarationSuffix() + String.format("[%d]", size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CArrayType other)) {
            return false;
        }
        return Objects.equals(elementType, other.elementType) &&
                size == other.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, size);
    }
}
