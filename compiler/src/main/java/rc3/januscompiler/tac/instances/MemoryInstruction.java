package rc3.januscompiler.tac.instances;

/**
 * This abstract class represents all {@link Instruction Instructions} that deal with memory.
 * <p>
 * It provides a specialization for the source and target {@link Operand} types and contains a {@link Constant}
 * offset.
 * </p>
 *
 * @param <T> The type of {@link Operand Operands} allowed as the target of this instruction.
 * @param <S> The type of {@link Operand Operands} allowed as the source of this instruction.
 */
public abstract class MemoryInstruction<T extends Operand, S extends Operand> extends Instruction {
    public final T target;
    public final S source;
    public final Constant offset;

    public MemoryInstruction(T target, S source, Constant offset) {
        this.target = target;
        this.source = source;
        this.offset = offset;
    }
}
