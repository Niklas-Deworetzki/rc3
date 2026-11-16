package rc3.januscompiler.tac.instances;

/**
 * This abstract superclass represents all {@link Instruction Instructions} where a
 * intermediate value is assigned to a name represented by a {@link TemporaryOperand}.
 *
 * @see AddressOfInstruction
 * @see ArithmeticInstruction
 * @see CopyInstruction
 */
public abstract class AssignmentInstruction extends Instruction {
    public final TemporaryOperand target;

    public AssignmentInstruction(TemporaryOperand target) {
        this.target = target;
    }
}
