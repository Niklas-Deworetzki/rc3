package rc3.stackmachine.syntax;

import java.util.List;

/**
 * {@link Line Lines} are the building blocks of a stack machine program.
 * <p>
 * Every {@link Line} either describes some {@link Instruction} or declares
 * {@link SetValue data}.
 * <p>
 * A {@link Line} can be associated with an arbitrary {@link List} of labels.
 */
public sealed interface Line permits Instruction, SetValue {
    /**
     * The {@link List} of labels associated with this {@link Line}.
     *
     * @implSpec This {@link List} must be mutable and backed by the
     * {@link List} of labels associated with this {@link Line}.
     */
    List<Operand.Symbol> getLabels();

    /**
     * Adds the given label to the {@link List} of labels associated with this {@link Line}.
     */
    default void addLabel(Operand.Symbol label) {
        getLabels().add(label);
    }

    /**
     * Return a {@link String} representation for this {@link Line} excluding labels.
     */
    String rawRepresentation();

    /**
     * Return a {@link String} representation for this {@link Line} including labels.
     */
    default String getRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (Operand.Symbol label : getLabels()) {
            sb.append(label).append(":\n");
        }
        sb.append(rawRepresentation());
        return sb.toString();
    }

    /**
     * Returns the inverse for a given {@link Line}.
     */
    @SuppressWarnings("unchecked")
    static <L extends Line> L reverse(L line) {
        if (line instanceof Instruction instruction) {
            return (L) instruction.reverse(); // A cast is needed here to satisfy the compiler.
        }
        return line;
    }

}
