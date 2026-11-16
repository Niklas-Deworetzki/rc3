package rc3.januscompiler.syntax.visitor;

import rc3.januscompiler.syntax.*;

/**
 * This class is a specialization of {@link TreeVisitor} for a <code>void</code> return type.
 * <p>
 * It would be required to <code>return null;</code> in every implemented method of the visitor,
 * so a <code>visitVoid</code> method is provided for every <code>visit</code> method of the tree visitor.
 * </p>
 */
public abstract class VoidTreeVisitor extends TreeVisitor<Void> {

    @Override
    public Void visit(AssignStatement assignStatement) {
        visitVoid(assignStatement);
        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryExpression) {
        visitVoid(binaryExpression);
        return null;
    }

    @Override
    public Void visit(BuiltinExpression builtinExpression) {
        visitVoid(builtinExpression);
        return null;
    }

    @Override
    public Void visit(BuiltinStatement builtinStatement) {
        visitVoid(builtinStatement);
        return null;
    }

    @Override
    public Void visit(CallStatement callStatement) {
        visitVoid(callStatement);
        return null;
    }

    @Override
    public Void visit(IfStatement ifStatement) {
        visitVoid(ifStatement);
        return null;
    }

    @Override
    public Void visit(IndexedVariable indexedVariable) {
        visitVoid(indexedVariable);
        return null;
    }

    @Override
    public Void visit(IntLiteral intLiteral) {
        visitVoid(intLiteral);
        return null;
    }

    @Override
    public Void visit(MainProcedureDeclaration mainProcedure) {
        visitVoid(mainProcedure);
        return null;
    }

    @Override
    public Void visit(LocalBlockStatement localBlockStatement) {
        visitVoid(localBlockStatement);
        return null;
    }

    @Override
    public Void visit(LoopStatement loopStatement) {
        visitVoid(loopStatement);
        return null;
    }

    @Override
    public Void visit(NamedVariable namedVariable) {
        visitVoid(namedVariable);
        return null;
    }

    @Override
    public Void visit(ProcedureDeclaration procedureDeclaration) {
        visitVoid(procedureDeclaration);
        return null;
    }

    @Override
    public Void visit(Program program) {
        visitVoid(program);
        return null;
    }

    @Override
    public Void visit(SkipStatement skipStatement) {
        visitVoid(skipStatement);
        return null;
    }

    @Override
    public Void visit(StackNilLiteral stackNilLiteral) {
        visitVoid(stackNilLiteral);
        return null;
    }

    @Override
    public Void visit(SwapStatement swapStatement) {
        visitVoid(swapStatement);
        return null;
    }

    @Override
    public Void visit(VariableDeclaration variableDeclaration) {
        visitVoid(variableDeclaration);
        return null;
    }

    @Override
    public Void visit(VariableExpression variableExpression) {
        visitVoid(variableExpression);
        return null;
    }

    public abstract void visitVoid(AssignStatement assignStatement);

    public abstract void visitVoid(BinaryExpression binaryExpression);

    public abstract void visitVoid(BuiltinExpression builtinExpression);

    public abstract void visitVoid(BuiltinStatement builtinStatement);

    public abstract void visitVoid(CallStatement callStatement);

    public abstract void visitVoid(IfStatement ifStatement);

    public abstract void visitVoid(IndexedVariable indexedVariable);

    public abstract void visitVoid(IntLiteral intLiteral);

    public abstract void visitVoid(MainProcedureDeclaration mainProcedure);

    public abstract void visitVoid(LocalBlockStatement localBlockStatement);

    public abstract void visitVoid(LoopStatement loopStatement);

    public abstract void visitVoid(NamedVariable namedVariable);

    public abstract void visitVoid(ProcedureDeclaration procedureDeclaration);

    public abstract void visitVoid(Program program);

    public abstract void visitVoid(SkipStatement skipStatement);

    public abstract void visitVoid(StackNilLiteral stackNilLiteral);

    public abstract void visitVoid(SwapStatement swapStatement);

    public abstract void visitVoid(VariableDeclaration variableDeclaration);

    public abstract void visitVoid(VariableExpression variableExpression);
}
