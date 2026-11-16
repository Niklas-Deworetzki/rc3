package rc3.januscompiler.tac.instances;

/**
 * This abstract class is the superclass of all operands that can hold variable values.
 */
public abstract class VariableOperand extends Operand {
    public final String name;

    public VariableOperand(String name) {
        this.name = name;
    }
}
