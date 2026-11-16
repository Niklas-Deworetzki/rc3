package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This instruction is used to store the address of an {@link VariableOperand} into another {@link Operand}.
 * <p>
 * In ECO32 this instruction would translate to an <code>add</code> instruction, adding the variable's offset
 * to the stack pointer.
 * </p>
 */
public class AddressOfInstruction extends AssignmentInstruction {
    public final VariableOperand variable;

    public AddressOfInstruction(TemporaryOperand target, VariableOperand variable) {
        super(target);
        this.variable = variable;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\t%s := AddressOf %s", target, variable);
    }
}
