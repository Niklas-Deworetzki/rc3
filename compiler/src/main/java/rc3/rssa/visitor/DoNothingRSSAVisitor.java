package rc3.rssa.visitor;

import rc3.rssa.instances.*;

import java.util.function.Supplier;

/**
 * This class implements every <code>visit</code> method of an {@link RSSAVisitor} returning a
 * predefined value unless a method is overridden.
 */
public class DoNothingRSSAVisitor<R> extends RSSAVisitor<R> {
    private final Supplier<R> defaultValue;

    /**
     * Initializes the visitor to return <code>null</code> in all non-overridden methods.
     */
    public DoNothingRSSAVisitor() {
        this((R) null);
    }

    /**
     * Initializes the visitor to return a predefined value in all non-overridden methods.
     *
     * @param defaultValue The value to return if a method was not overridden.
     */
    public DoNothingRSSAVisitor(R defaultValue) {
        this.defaultValue = () -> defaultValue;
    }

    /**
     * Initializes the visitor to return a new value each time a non-overridden method is called.
     *
     * @param defaultValueSupplier The {@link Supplier} for default values.
     */
    public DoNothingRSSAVisitor(Supplier<R> defaultValueSupplier) {
        this.defaultValue = defaultValueSupplier;
    }


    @Override
    public R visit(ArithmeticAssignment arithmeticInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(BeginInstruction beginInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(BinaryOperand binaryOperand) {
        return defaultValue.get();
    }

    @Override
    public R visit(CallInstruction callInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(ConditionalEntry conditionalEntry) {
        return defaultValue.get();
    }

    @Override
    public R visit(ConditionalExit conditionalExit) {
        return defaultValue.get();
    }

    @Override
    public R visit(Constant constant) {
        return defaultValue.get();
    }

    @Override
    public R visit(EndInstruction endInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(MemoryAccess memoryAccess) {
        return defaultValue.get();
    }

    @Override
    public R visit(MemoryAssignment memoryAssignment) {
        return defaultValue.get();
    }

    @Override
    public R visit(MemoryInterchangeInstruction memoryInterchangeInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(MemorySwapInstruction memorySwapInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(SwapInstruction swapInstruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(UnconditionalEntry unconditionalEntry) {
        return defaultValue.get();
    }

    @Override
    public R visit(UnconditionalExit unconditionalExit) {
        return defaultValue.get();
    }

    @Override
    public R visit(Variable variable) {
        return defaultValue.get();
    }
}
