package rc3.januscompiler.syntax.visitor;

import rc3.januscompiler.syntax.*;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This abstract class is a skeleton for concrete {@link TreeVisitor} instances.
 * <p>
 * {@link Consumer} and {@link Function} is implemented by this abstract class, so visitor instances can be
 * used with modern functional Java constructs.
 * </p>
 *
 * @param <R> The type of the value returned by visiting an {@link VisitableTree}.
 */
public abstract class TreeVisitor<R> implements Consumer<VisitableTree>, Function<VisitableTree, R> {

    @Override
    public void accept(VisitableTree visitable) {
        visitable.accept(this);
    }

    @Override
    public R apply(VisitableTree visitable) {
        return visitable.accept(this);
    }

    public abstract R visit(AssignStatement assignStatement);

    public abstract R visit(BinaryExpression binaryExpression);

    public abstract R visit(BuiltinExpression builtinExpression);

    public abstract R visit(BuiltinStatement builtinStatement);

    public abstract R visit(CallStatement callStatement);

    public abstract R visit(IfStatement ifStatement);

    public abstract R visit(IndexedVariable indexedVariable);

    public abstract R visit(IntLiteral intLiteral);

    public abstract R visit(MainProcedureDeclaration mainProcedure);

    public abstract R visit(LocalBlockStatement localBlockStatement);

    public abstract R visit(NamedVariable namedVariable);

    public abstract R visit(ProcedureDeclaration procedureDeclaration);

    public abstract R visit(Program program);

    public abstract R visit(SkipStatement skipStatement);

    public abstract R visit(StackNilLiteral stackNilLiteral);

    public abstract R visit(SwapStatement swapStatement);

    public abstract R visit(VariableDeclaration variableDeclaration);

    public abstract R visit(VariableExpression variableExpression);

    public abstract R visit(LoopStatement loopStatement);
}
