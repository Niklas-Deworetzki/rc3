package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} represents an unconditional jump to the given {@link Label}.
 * <p>
 * In ECO32 this instruction would translate to the unconditional <code>j</code> instruction.
 * </p>
 */
public class GotoInstruction extends JumpingInstruction {

    public GotoInstruction(String label) {
        super(label);
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\tGoto %s", label);
    }
}
