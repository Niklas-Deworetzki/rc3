package rc3.rssa.translation;

import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.types.ArrayType;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.Variable;
import rc3.rssa.instances.*;

import java.util.Map;

import static rc3.rssa.instances.BinaryOperand.BinaryOperator;

public final class ExpressionRSSAGenerator extends DoNothingTreeVisitor<Value> {
    private final RSSAGenerator generator;

    public ExpressionRSSAGenerator(RSSAGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Value visit(BinaryExpression binaryExpression) {
        Value left = binaryExpression.lhs.accept(this);
        Value right = binaryExpression.rhs.accept(this);

        return new BinaryOperand(
                generator.promoteToRValue(left),
                translateOperator(binaryExpression.operator),
                generator.promoteToRValue(right));
    }

    public static BinaryOperand.BinaryOperator translateOperator(BinaryExpression.BinaryOperator operator) {
        return switch (operator) {
            case ADD -> BinaryOperator.ADD;
            case SUB -> BinaryOperator.SUB;
            case DIV -> BinaryOperator.DIV;
            case MUL -> BinaryOperator.MUL;
            case MOD -> BinaryOperator.MOD;
            case XOR -> BinaryOperator.XOR;
            case LST -> BinaryOperator.LST;
            case LSE -> BinaryOperator.LSE;
            case GRT -> BinaryOperator.GRT;
            case GRE -> BinaryOperator.GRE;
            case EQU -> BinaryOperator.EQU;
            case NEQ -> BinaryOperator.NEQ;
            case AND, CONJ -> BinaryOperator.AND;
            case OR, DISJ -> BinaryOperator.OR;
        };
    }

    @Override
    public Value visit(BuiltinExpression builtinExpression) {
        StackVariable stackVariable = new StackVariable(generator, builtinExpression.argumentList.get(0).name);
        return switch (builtinExpression.expression) {
            case TOP -> stackVariable.getTopElement();
            case EMPTY -> stackVariable.isEmptyStack();
        };
    }

    private int getArraySize(IndexedVariable indexedVariable) {
        VariableEntry entry = generator.getEnvironment()
                .getVariable(indexedVariable.name)
                .orElseThrow(ErrorMessage.newInternalError(
                        "Encountered unknown array named '%s'!", indexedVariable.name));

        return ((ArrayType) entry.type).size;
    }

    @Override
    public Value visit(IndexedVariable indexedVariable) {
        Variable accessPosition = Variable.nextTemporaryVariable();
        Variable array = generator.fromName(indexedVariable.name);

        // Idx := E [[ Var.Index ]]
        Atom index = generator.promoteToAtom(
                generator.translateExpression(indexedVariable.index));

        // Pos := 0 + Var.Offset + Idx
        Instruction calculateMemoryPosition = ArithmeticAssignment.assignWithoutDestroy(
                accessPosition, new BinaryOperand(array, BinaryOperator.ADD, index));
        generator.emit(calculateMemoryPosition);

        // Destroy with:  0 := Pos - Idx + Var.Offset
        generator.enqueueDestruction(calculateMemoryPosition.reverse());

        // M[Pos]
        MemoryAccess memoryAccess = new MemoryAccess(accessPosition);
        memoryAccess.annotate(new Annotation("InBounds", Map.of(
                "base", array,
                "size", new Constant(getArraySize(indexedVariable)))));
        return memoryAccess;
    }

    @Override
    public Value visit(StackNilLiteral stackNilLiteral) {
        // Literal nil does not have any code associated with it. Its usage is detected in and handled elsewhere.
        throw new ErrorMessage.UnsupportedOperation("RSSA-Backend does not support stacks.", stackNilLiteral.position);
    }

    @Override
    public Value visit(IntLiteral intLiteral) {
        return new Constant(intLiteral.value);
    }

    @Override
    public Value visit(NamedVariable namedVariable) {
        return generator.fromName(namedVariable.name);
    }

    @Override
    public Value visit(VariableExpression variableExpression) {
        return variableExpression.variable.accept(this);
    }

}
