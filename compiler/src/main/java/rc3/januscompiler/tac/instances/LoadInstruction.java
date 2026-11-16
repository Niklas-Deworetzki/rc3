package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * Loads a value from the memory address represented by an {@link Operand} into a {@link TemporaryOperand}.
 * <p>
 * A {@link Constant} offset can be added to the source memory address.
 * </p>
 * <p>
 * In ECO32 this instruction would translate to the <code>ldw</code> instruction.
 * </p>
 */
public class LoadInstruction extends MemoryInstruction<TemporaryOperand, Operand> {

    public LoadInstruction(TemporaryOperand target, Operand source) {
        super(target, source, new Constant(0));
    }

    public LoadInstruction(TemporaryOperand target, Operand source, Constant offset) {
        super(target, source, offset);
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return (offset.hasKnownIntValue() && offset.getKnownIntValue() == 0)
                ? String.format("\t%s := *%s", target, source)
                : String.format("\t%s := *(%s + %s)", target, source, offset);
    }
}
