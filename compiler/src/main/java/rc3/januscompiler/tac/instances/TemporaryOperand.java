package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Operand} represents the intermediate values generated during the evaluation of expressions.
 * <p>
 * {@link TemporaryOperand TemporaryOperands} have a name that uniquely identifies them.
 * </p>
 */
public class TemporaryOperand extends VariableOperand {

    public TemporaryOperand(String name) {
        super(name);
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
