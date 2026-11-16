package rc3.januscompiler.tac.instances;

import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This instruction prepares a parameter to be passed to the next call.
 * The offset of the passed parameter is stored in the {@link ParameterType} passed in the constructor.
 */
public class ParamInstruction extends Instruction {
    public final ParameterType parameterType;
    public final Operand operand;

    public ParamInstruction(ParameterType parameterType, Operand operand) {
        this.parameterType = parameterType;
        this.operand = operand;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\tParam %s,%d", operand, parameterType.offset);
    }
}
