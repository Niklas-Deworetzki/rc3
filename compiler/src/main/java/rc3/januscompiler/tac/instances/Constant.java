package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.TacGenerator;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * This class represent constant values in the Three-Address-Code. {@link Constant}
 * values are values that are known <i>before runtime</i>. In contrast to {@link Operand}
 * values, that are only known <i>at runtime</i>.
 * <p>
 * In the most cases, it is sufficient to provide an {@link Integer} value as a constant.
 * With this class it is also possible to make use of macros, constant evaluators, etc. that are
 * part of the toolchain used.
 * </p>
 * <p>
 * As an example, it may be desired to use the size of an <code>int</code> as part of a calculation
 * but the code generated is used in C, where there is no fixed size for the <code>int</code> type.
 * <pre>
 * new LoadInstruction(value, array, new Constant("sizeof(int)"));
 * </pre>
 * The above Java code could be used to reference <code>sizeof(int)</code>, which will be evaluated
 * by the C compiler.
 * </p>
 * <p>
 * When analyzing {@link Instruction Instructions} it sometimes is of interest, to know the value of
 * a constant as this allows certain kinds of optimizations. For these cases some accessor methods
 * are provided to request and update the value of this {@link Constant}.
 * </p>
 */
public class Constant {
    /**
     * The {@link String} representation of this {@link Constant}. If this value represents a value
     * known by the {@link TacGenerator} and not for use in later parts of
     * the toolchain, it can be extracted using {@link Constant#knownIntValue()}.
     */
    public final String representation;
    private Integer value;

    /**
     * Creates a {@link Constant} using an <code>int</code> value. This value can later be extracted using
     * {@link Constant#knownIntValue()}.
     *
     * @param value The <code>int</code> value of this constant.
     */
    public Constant(int value) {
        this.representation = Integer.toString(value);
        this.value = value;
    }

    /**
     * Creates a {@link Constant} with a fixed <code>int</code> value and representation. This constructor
     * can be used, to enforce a certain representation of the given <code>int</code> value in later stages.
     * <p>
     * In this example, this constructor is used to enforce hexadecimal representation of an <code>int</code> value.
     * <pre>
     * new Constant("0x0A", 10);
     * </pre>
     * </p>
     *
     * @param representation The representation for this constant. This can be freely chosen to enforce
     *                       the representation during later stages.
     * @param value          The actual <code>int</code> value of this constant.
     * @implNote Both representation and value should describe the same value.
     */
    public Constant(String representation, int value) {
        this.representation = Objects.requireNonNull(representation);
        this.value = value;
    }

    /**
     * Creates a {@link Constant} without a fixed <code>int</code> value. Only the representation is known
     * and the actual value is evaluated on during later stages. This allows to interface with macros or
     * evaluators on later stages of a toolchain.
     *
     * @param representation The representation of this {@link Constant}.
     */
    public Constant(String representation) {
        this.representation = Objects.requireNonNull(representation);
        this.value = null;
    }

    /**
     * Returns the {@link String} representation of this {@link Constant}.
     *
     * @return String representation of this {@link Constant}.
     */
    public String getRepresentation() {
        return representation;
    }

    /**
     * Checks whether this {@link Constant} represents a <code>int</code> value that is known by the
     * {@link TacGenerator}.
     *
     * @return <code>true</code> if a value is known for this {@link Constant}. Otherwise <code>false</code>
     * is returned, meaning that the represented value has to be evaluated later.
     */
    public boolean hasKnownIntValue() {
        return value != null;
    }

    /**
     * Returns the <code>int</code> value of this {@link Constant} if one is present. Otherwise an empty
     * result is returned.
     *
     * @return The represented value or empty.
     */
    public OptionalInt knownIntValue() {
        return (value != null) ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public int getKnownIntValue() throws NoSuchElementException {
        if (value == null) {
            throw new NoSuchElementException("The value of " + this + " is not known yet!");
        }
        return value;
    }

    /**
     * Sets the <code>int</code> value that this {@link Constant} represents.
     *
     * @param value The new value of this {@link Constant}.
     */
    public void updateKnownValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return representation;
    }

    @Override
    public int hashCode() {
        return representation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof Constant &&
                ((Constant) obj).representation.equals(representation);
    }
}
