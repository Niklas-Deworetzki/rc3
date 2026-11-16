package rc3.januscompiler.backend.tac.types;

import rc3.januscompiler.types.PrimitiveType;

import java.util.Objects;

/**
 * Instances of this class represent the C version of Janus' {@link PrimitiveType}.
 */
public final class CPrimitiveType extends CType {
    private final String name;
    private final String initialValue;

    private CPrimitiveType(String name, String initialValue) {
        this.name = name;
        this.initialValue = initialValue;
    }

    /**
     * The primitive type <code>int</code>. Variables of this type are initialized with <code>0</code>.
     */
    public static final CPrimitiveType IntType = new CPrimitiveType("int", "0");
    /**
     * The primitive type <code>stack</code>. This type is defined as a pointer to a stack cell.
     * Variables of this type are initialized with <code>nil</code>.
     */
    public static final CPrimitiveType StackType = new CPrimitiveType("stack", "nil");

    @Override
    public String getName() {
        return "_" + name;
    }

    @Override
    public String getInitialValue() {
        return initialValue;
    }

    @Override
    protected String declarationPrefix() {
        return name + " ";
    }

    @Override
    protected String declarationSuffix() {
        return "";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, initialValue);
    }
}
