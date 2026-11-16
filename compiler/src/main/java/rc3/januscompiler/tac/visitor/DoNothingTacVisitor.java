package rc3.januscompiler.tac.visitor;

import rc3.januscompiler.tac.instances.*;

import java.util.function.Supplier;

/**
 * This class implements every <code>visit</code> method of an {@link TacVisitor} returning a
 * predefined value unless a method is overridden.
 */
public class DoNothingTacVisitor<R> extends TacVisitor<R> {
    private final Supplier<R> defaultValue;

    /**
     * Initializes the visitor to return <code>null</code> in all non-overridden methods.
     */
    public DoNothingTacVisitor() {
        this((R) null);
    }

    /**
     * Initializes the visitor to return a predefined value in all non-overridden methods.
     *
     * @param defaultValue The value to return if a method was not overridden.
     */
    public DoNothingTacVisitor(R defaultValue) {
        this.defaultValue = () -> defaultValue;
    }

    /**
     * Initializes the visitor to return a new value each time a non-overridden method is called.
     *
     * @param defaultValueSupplier The {@link Supplier} for default values.
     */
    public DoNothingTacVisitor(Supplier<R> defaultValueSupplier) {
        this.defaultValue = defaultValueSupplier;
    }

    @Override
    public R visit(AddressOfInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(ArithmeticInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(BeginInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(CallInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(ConstantOperand operand) {
        return defaultValue.get();
    }

    @Override
    public R visit(CopyInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(EndInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(GotoInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(IfGotoInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(Label label) {
        return defaultValue.get();
    }

    @Override
    public R visit(LoadInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(NamedOperand operand) {
        return defaultValue.get();
    }

    @Override
    public R visit(ParamInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(ReturnInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(StoreInstruction instruction) {
        return defaultValue.get();
    }

    @Override
    public R visit(TemporaryOperand operand) {
        return defaultValue.get();
    }
}
