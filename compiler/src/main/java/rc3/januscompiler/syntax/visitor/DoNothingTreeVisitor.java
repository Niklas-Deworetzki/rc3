package rc3.januscompiler.syntax.visitor;

import rc3.januscompiler.syntax.*;

import java.util.function.Supplier;

/**
 * This class implements every <code>visit</code> method of an {@link TreeVisitor} returning a
 * predefined value unless a method is overridden.
 */
public class DoNothingTreeVisitor<R> extends TreeVisitor<R> {
    private final Supplier<R> defaultValue;

    /**
     * Initializes the visitor to return <code>null</code> in all non-overridden methods.
     */
    public DoNothingTreeVisitor() {
        this((R) null);
    }

    /**
     * Initializes the visitor to return a predefined value in all non-overridden methods.
     *
     * @param defaultValue The value to return if a method was not overridden.
     */
    public DoNothingTreeVisitor(R defaultValue) {
        this.defaultValue = () -> defaultValue;
    }

    /**
     * Initializes the visitor to return a new value each time a non-overridden method is called.
     *
     * @param defaultValueSupplier The {@link Supplier} for default values.
     */
    public DoNothingTreeVisitor(Supplier<R> defaultValueSupplier) {
        this.defaultValue = defaultValueSupplier;
    }

    @Override
    public R visit(AssignStatement assignStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(BinaryExpression binaryExpression) {
        return defaultValue.get();
    }

    @Override
    public R visit(BuiltinExpression builtinExpression) {
        return defaultValue.get();
    }

    @Override
    public R visit(BuiltinStatement builtinStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(CallStatement callStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(IfStatement ifStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(IndexedVariable indexedVariable) {
        return defaultValue.get();
    }

    @Override
    public R visit(IntLiteral intLiteral) {
        return defaultValue.get();
    }

    @Override
    public R visit(MainProcedureDeclaration mainProcedure) {
        return defaultValue.get();
    }

    @Override
    public R visit(LocalBlockStatement localBlockStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(NamedVariable namedVariable) {
        return defaultValue.get();
    }

    @Override
    public R visit(ProcedureDeclaration procedureDeclaration) {
        return defaultValue.get();
    }

    @Override
    public R visit(Program program) {
        return defaultValue.get();
    }

    @Override
    public R visit(SkipStatement skipStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(StackNilLiteral stackNilLiteral) {
        return defaultValue.get();
    }

    @Override
    public R visit(SwapStatement swapStatement) {
        return defaultValue.get();
    }

    @Override
    public R visit(VariableDeclaration variableDeclaration) {
        return defaultValue.get();
    }

    @Override
    public R visit(VariableExpression variableExpression) {
        return defaultValue.get();
    }

    @Override
    public R visit(LoopStatement loopStatement) {
        return defaultValue.get();
    }
}
