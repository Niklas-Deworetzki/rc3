package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} returns the control flow to the calling procedure.
 * <p>
 * In ECO32 this instruction would translate to the <code>jr</code> instruction that
 * can be used to jump to the address stored in the <code>return address register</code>.
 * </p>
 */
public class ReturnInstruction extends Instruction {

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "\tReturn";
    }
}
