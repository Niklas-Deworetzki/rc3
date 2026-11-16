package rc3.rssa.translation;

import rc3.januscompiler.syntax.IndexedVariable;
import rc3.januscompiler.syntax.NamedVariable;
import rc3.januscompiler.table.Identifier;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.*;

import java.util.Map;

import static rc3.januscompiler.syntax.AssignStatement.ModificationOperator;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator.ADD;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator.EQU;

/**
 * This class capsules different stack operations to provide a single operand for them.
 * <p>
 * A stack is implemented by implicitly carrying around two variables:
 * <ul>
 *  <li>The <b>reference</b>; a pointer to the base memory location of the stack.</li>
 *  <li>The <b>top</b>; holding the amount of elements on the stack and providing a memory offset for new elements.</li>
 * </ul>
 * </p>
 * <p>
 * At creation of a {@link StackVariable} a {@link RSSAGenerator} must be passed, which is then used to
 * generate instructions for different usage scenarios.
 * </p>
 */
public final class StackVariable {
    private final RSSAGenerator generator;
    private final String baseName;

    public StackVariable(RSSAGenerator generator, Identifier stack) {
        this.generator = generator;
        this.baseName = generator.fromName(stack).getRawName();
    }

    /**
     * Returns a {@link Variable} holding the value of this stacks base address.
     *
     * @implNote This {@link Variable} should not be destroyed.
     */
    public Variable getStackReference() {
        return new Variable(baseName + "_ref");
    }

    /**
     * Returns a {@link Variable} holding the amount of elements on the stack.
     *
     * @implNote This {@link Variable} should not be destroyed.
     */
    public Variable getStackTop() {
        return new Variable(baseName + "_top");
    }


    private int getReservedStackSize() {
        return generator.getMemoryManagement().getReservedStackSize();
    }

    private ArithmeticAssignment assignStackAddress(Variable variable, int offset) {
        // Addr := offset + (Stack_Ref + Stack_Top)
        return new ArithmeticAssignment(
                variable, new Constant(offset), ModificationOperator.ADD,
                getStackReference(), ADD, getStackTop());
    }

    private ArithmeticAssignment assignZeroBasedAddress(Variable variable, Atom index) {
        // Addr := 0 + (Stack_Ref + index)
        return new ArithmeticAssignment(
                variable, new Constant(0), ModificationOperator.ADD,
                getStackReference(), ADD, index);
    }

    private Annotation inStackAnnotation() {
        return new Annotation("InBounds", Map.of(
                "base", getStackReference(),
                "size", new Constant(getReservedStackSize())));
    }

    private MemoryAccess accessStackAt(Atom address) {
        MemoryAccess memoryAccess = new MemoryAccess(address);
        memoryAccess.annotate(inStackAnnotation());
        return memoryAccess;
    }

    /**
     * Returns a {@link MemoryAccess} for the highest element on the stack.
     *
     * @implNote Invocation of this method produces garbage that is collectible via
     * {@link RSSAGenerator#emitPendingDestruction()}.
     */
    public MemoryAccess getTopElement() {
        Variable topElementAddress = Variable.nextTemporaryVariable();
        var fetchTopAddress = assignStackAddress(topElementAddress, -1);

        // Addr := -1 + (Stack_Ref + Stack_Top)
        generator.emit(fetchTopAddress);
        // -1 := Addr - (Stack_Ref + Stack_Top)
        generator.enqueueDestruction(fetchTopAddress.reverse());

        return accessStackAt(topElementAddress);
    }

    /**
     * Returns a comparison checking whether this stack is empty.
     * <p>
     * Checking for an empty stack is achieved by using the amount of elements stored on the
     * stack, which is provided by {@link StackVariable#getStackTop()}.
     * </p>
     *
     * @implNote Invocation of this method does <b>not</b> produce garbage.
     */
    public BinaryOperand isEmptyStack() {
        return new BinaryOperand(getStackTop(), EQU, new Constant(0));
    }

    public void push(rc3.januscompiler.syntax.Variable variable) {
        Variable freeElementAddr = Variable.nextTemporaryVariable();
        var acquireAddress = assignStackAddress(freeElementAddr, 0);

        generator.emit(acquireAddress);

        if (variable instanceof IndexedVariable) {
            MemoryAccess varToPush = generator.fromIndexedVariable((IndexedVariable) variable);

            // M[Idx] <-> M[Addr]
            generator.emit(new MemorySwapInstruction(
                    varToPush, accessStackAt(freeElementAddr)));

            // Check if the variable pushed is now zero.
            generator.emit(new ArithmeticAssignment(
                    new Constant(0), new Constant(0), ModificationOperator.XOR,
                    varToPush, BinaryOperator.XOR, new Constant(0)));
        } else {
            Variable varToPush = generator.fromNamedVariable((NamedVariable) variable);

            // Var := M[Addr] := Var
            generator.emit(new MemoryInterchangeInstruction(
                    varToPush, accessStackAt(freeElementAddr), varToPush));

            // Check if the variable pushed is now zero.
            generator.emit(new ArithmeticAssignment(
                    new Constant(0), new Constant(0), ModificationOperator.XOR,
                    varToPush, BinaryOperator.XOR, new Constant(0)));
        }

        generator.emitPendingDestruction();
        generator.emit(acquireAddress.reverse());

        // Stack_Top := Stack_Top + (1)
        generator.emit(new ArithmeticAssignment(
                getStackTop(), getStackTop(), ModificationOperator.ADD,
                BinaryOperand.of(new Constant(1))));
    }

    public void pop(rc3.januscompiler.syntax.Variable variable) {
        generator.emitReversed(() -> this.push(variable));
    }

    public void alloc() {
        // Stack_Ref := <alloc>
        generator.alloc(getStackReference(), getReservedStackSize());

        // Stack_Top := 0
        generator.emit(ArithmeticAssignment.assignWithoutDestroy(
                getStackTop(),
                BinaryOperand.of(new Constant(0))));
    }

    public void free() {
        generator.emitReversed(this::alloc);
    }


    public void copyFrom(StackVariable other) {
        String labelSkip = generator.generateLabel();
        String labelBeginCopy = generator.generateLabel();
        String labelCopy = generator.generateLabel();
        String labelLoop = generator.generateLabel();
        String labelEnd = generator.generateLabel();
        String labelFromCopy = generator.generateLabel();
        Variable index = Variable.nextTemporaryVariable();

        // Stack_Ref := <new>
        generator.alloc(getStackReference(), getReservedStackSize());

        // Other_Top == 0 -> Skip()BeginCopy
        generator.emit(new ConditionalExit(labelSkip, labelBeginCopy,
                other.getStackTop(), EQU, new Constant(0)));
        {
            // BeginCopy <-
            generator.emit(new UnconditionalEntry(labelBeginCopy));
            // Index := 0 + 0
            generator.emit(ArithmeticAssignment.assignAndDestroy(index, new Constant(0)));
            // -> Copy
            generator.emit(new UnconditionalExit(labelCopy));
            {
                // Copy()Loop <- Index == 0
                generator.emit(new ConditionalEntry(labelCopy, labelLoop,
                        index, EQU, new Constant(0)));

                // Fetch addresses.
                Variable otherAddr = Variable.nextTemporaryVariable();
                var fetchOtherAddr = other.assignZeroBasedAddress(otherAddr, index);
                Variable stackAddr = Variable.nextTemporaryVariable();
                var fetchStackAddr = this.assignZeroBasedAddress(stackAddr, index);
                generator.emit(fetchOtherAddr);
                generator.emit(fetchStackAddr);

                // El := 0 ^ Other[Index]
                Variable element = generator.emitCopyAssignment(accessStackAt(otherAddr));
                // 0 := Stack[Index] := El
                generator.emit(new MemoryInterchangeInstruction(
                        new Constant(0), accessStackAt(stackAddr), element));

                // Destroy addresses.
                generator.emit(fetchStackAddr.reverse());
                generator.emit(fetchOtherAddr.reverse());

                // Index := Index + 1
                generator.emit(new ArithmeticAssignment(
                        index, index, ModificationOperator.ADD,
                        BinaryOperand.of(new Constant(1))));

                // Index == Other_Top -> End()Loop
                generator.emit(new ConditionalExit(labelEnd, labelLoop,
                        index, EQU, other.getStackTop()));
            }
            // <- End
            generator.emit(new UnconditionalEntry(labelEnd));
            // 0 := Index - Other_Top
            generator.emit(ArithmeticAssignment.newInstruction(new Constant(0),
                    index, ModificationOperator.SUB, other.getStackTop()));
            // -> FromCopy
            generator.emit(new UnconditionalExit(labelFromCopy));
        }
        // Skip()FromCopy <- Other_Top == 0
        generator.emit(new ConditionalEntry(labelSkip, labelFromCopy,
                other.getStackTop(), EQU, new Constant(0)));

        // Stack_Top := 0 + Other_Top
        generator.emit(ArithmeticAssignment.newInstruction(this.getStackTop(),
                new Constant(0), ModificationOperator.ADD, other.getStackTop()));
    }

    public void destroyWith(StackVariable other) {
        generator.emitReversed(() -> copyFrom(other));
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StackVariable)) return false;
        return this.baseName.equals(((StackVariable) obj).baseName);
    }

    @Override
    public int hashCode() {
        return baseName.hashCode();
    }

    @Override
    public String toString() {
        return String.format("StackVariable(%s)", baseName);
    }
}
