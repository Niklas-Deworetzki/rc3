package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} stores the value contained in an {@link Operand} at the memory address
 * contained in the other {@link Operand}. A {@link Constant} offset can be added to the memory address.
 * <p>
 * In ECO32 this instruction would translate to the <code>stw</code> instruction.
 * </p>
 */
public class StoreInstruction extends MemoryInstruction<Operand, Operand> {

    public StoreInstruction(Operand target, Operand source) {
        super(target, source, new Constant(0));
    }

    public StoreInstruction(Operand target, Constant offset, Operand source) {
        super(target, source, offset);
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return (offset.hasKnownIntValue() && offset.getKnownIntValue() == 0)
                ? String.format("\t*%s := %s", target, source)
                : String.format("\t*(%s + %s) := %s", target, offset, source);
    }
}
