package rc3.stackmachine.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SetValue} directive assigns a fixed value to a label or a memory address.
 */
public record SetValue(List<Operand.Symbol> labels, Operand memoryAddress, Operand value) implements Line {
    public SetValue(int memoryAddress, int value) {
        this(new ArrayList<>(0), new Operand.Constant(memoryAddress), new Operand.Constant(value));
    }

    @Override
    public List<Operand.Symbol> getLabels() {
        return labels;
    }

    @Override
    public String rawRepresentation() {
        return String.format(".set %s %s", memoryAddress, value);
    }
}
