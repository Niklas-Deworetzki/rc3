package rc3.januscompiler.tac.instances;

/**
 * This abstract class represents all {@link Instruction Instructions} that change the execution path.
 *
 * @see CallInstruction
 * @see GotoInstruction
 * @see IfGotoInstruction
 */
public abstract class JumpingInstruction extends Instruction {
    public final String label;

    public JumpingInstruction(String label) {
        this.label = label;
    }
}
