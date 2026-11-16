package rc3.januscompiler.table;

import rc3.januscompiler.backend.VariableAllocator;
import rc3.januscompiler.types.Type;

import java.util.Objects;

/**
 * This class holds information required for parameters.
 * <p>
 * Additionally to the {@link Type} of an parameter, its offset is also stored.
 * </p>
 */
public final class ParameterType {
    /**
     * The {@link Type} of this parameter.
     */
    public final Type type;
    /**
     * The offset of this parameter in the stack frame.
     *
     * @implNote This information has to be filled in by a {@link VariableAllocator}.
     */
    public int offset;

    public ParameterType(Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ParameterType other)) {
            return false;
        } else {
            return offset == other.offset &&
                    Objects.equals(type, other.type);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, offset);
    }

    @Override
    public String toString() {
        return String.format("ParameterType{type=%s, offset=%d}", type, offset);
    }
}
