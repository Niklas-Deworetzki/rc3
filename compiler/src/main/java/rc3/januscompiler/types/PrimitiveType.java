package rc3.januscompiler.types;

import java.util.Objects;

/**
 * Instances of this class represent the primitive types builtin into Janus type system.
 */
public final class PrimitiveType extends Type {
    /**
     * The <code>int</code> type represents integer numbers from <code>-2³¹</code> to <code>2³¹-1</code>.
     */
    public static final PrimitiveType IntType = new PrimitiveType("int");
    /**
     * The <code>stack</code> type represents a potentially unlimited storage for integer variables which are
     * accessible in LIFO order.
     */
    public static final PrimitiveType StackType = new PrimitiveType("stack");
    /**
     * The <code>bool</code> type represents the result of conditional expressions. It is not possible to
     * create a variable of this type in Janus.
     */
    public static final PrimitiveType BoolType = new PrimitiveType("bool");

    /**
     * The name is used to identify the {@link PrimitiveType} instance.
     */
    private final String name;

    private PrimitiveType(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof PrimitiveType other &&
                Objects.equals(other.name, name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
