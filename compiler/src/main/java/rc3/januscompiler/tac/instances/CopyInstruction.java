package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} assigns the value of an {@link Operand} to another {@link Operand}.
 * <p>
 * In ECO32 this instruction would translate to an addition with <code>0</code> or the register <code>$0</code>.
 * </p>
 */
public class CopyInstruction extends AssignmentInstruction {
    public final Operand source;

    public CopyInstruction(TemporaryOperand target, Operand source) {
        super(target);
        this.source = source;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\t%s := %s", target, source);
    }
}
