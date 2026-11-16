package rc3.januscompiler.tac.instances;

import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Operand} represents variables used as an operand. The variable represented by this
 * {@link Operand} is determined by the {@link VariableEntry} passed in the constructor.
 */
public class NamedOperand extends VariableOperand {
    public final VariableEntry entry;

    public NamedOperand(VariableEntry entry, String name) {
        super(name);
        this.entry = entry;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
