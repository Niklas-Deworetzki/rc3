package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} represents the end of a procedure.
 * <p>
 * A translation of this {@link Instruction} to executable code has to destruct the active stack frame.
 * </p>
 */
public class EndInstruction extends Instruction {

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "\tEnd";
    }
}
