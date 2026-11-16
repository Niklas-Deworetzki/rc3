package rc3.januscompiler.types;

import java.util.Objects;

/**
 * Instances of this class represent an array in Janus type system.
 * <p>
 * Arrays always have a fixed size of integer elements.
 * </p>
 */
public final class ArrayType extends Type {
    /**
     * The amount of integers stored in an instance of this {@link Type}.
     */
    public final int size;

    public ArrayType(int size) {
        this.size = size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ArrayType.class, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof ArrayType && ((ArrayType) obj).size == this.size;
    }

    @Override
    public String toString() {
        return String.format("int[%d]", size);
    }
}
