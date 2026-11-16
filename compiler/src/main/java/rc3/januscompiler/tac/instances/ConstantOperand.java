package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This class represents {@link Operand Operands} with a constant value.
 *
 * @see Constant
 */
public class ConstantOperand extends Operand {
    public final Constant value;

    public ConstantOperand(int value) {
        this.value = new Constant(value);
    }

    public ConstantOperand(Constant value) {
        this.value = value;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
