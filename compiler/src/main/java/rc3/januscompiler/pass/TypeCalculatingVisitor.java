package rc3.januscompiler.pass;

import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;
import rc3.lib.messages.ErrorMessage;

import java.util.Iterator;
import java.util.Objects;

/**
 * This {@link TreeVisitor} implementation is used to
 * calculate the {@link Type} of {@link Expression Expressions} and {@link Variable Variables}
 * under a given {@link Environment}.
 */
public class TypeCalculatingVisitor extends DoNothingTreeVisitor<Type> {
    public final Environment environment;

    public TypeCalculatingVisitor(Environment environment) {
        this.environment = environment;
    }

    private void expectOperandType(Type expectedOperandType, Expression operand) {
        Type operandType = operand.getDataType(environment);

        if (!expectedOperandType.equals(operandType)) {
            throw new ErrorMessage.OperatorTypeMismatch(operand, expectedOperandType, operandType);
        }
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        switch (binaryExpression.operator.getType()) {
            case ARITHMETIC, BITWISE -> {
                expectOperandType(PrimitiveType.IntType, binaryExpression.lhs);
                expectOperandType(PrimitiveType.IntType, binaryExpression.rhs);
                return PrimitiveType.IntType;
            }
            case LOGIC -> {
                expectOperandType(PrimitiveType.BoolType, binaryExpression.lhs);
                expectOperandType(PrimitiveType.BoolType, binaryExpression.rhs);
                return PrimitiveType.BoolType;
            }
            case RELATIONAL -> {
                expectOperandType(PrimitiveType.IntType, binaryExpression.lhs);
                expectOperandType(PrimitiveType.IntType, binaryExpression.rhs);
                return PrimitiveType.BoolType;
            }
            default -> throw new RuntimeException("Unknown operator type: " + binaryExpression.operator.getType());
        }
    }

    @Override
    public Type visit(BuiltinExpression builtinExpression) {
        Iterator<Variable> arguments = builtinExpression.argumentList.iterator();
        Iterator<Type> requiredTypes = builtinExpression.expression.getRequiredParameterTypes().iterator();

        while (arguments.hasNext() && requiredTypes.hasNext()) {
            var argument = arguments.next();
            var argumentType = argument.getDataType(environment);
            var requiredType = requiredTypes.next();

            if (!Objects.equals(requiredType, argumentType)) {
                String description = String.format("Operand of '%s'", builtinExpression.expression.getName());
                throw new ErrorMessage.RequiresDifferentType(description, argument.position, requiredType, argumentType);
            }
        }

        return builtinExpression.expression.getReturnType();
    }

    @Override
    public Type visit(IntLiteral intLiteral) {
        // IntLiterals are always an integer.
        return PrimitiveType.IntType;
    }

    @Override
    public Type visit(StackNilLiteral stackNilLiteral) {
        // The empty stack is always a stack.
        return PrimitiveType.StackType;
    }

    @Override
    public Type visit(VariableExpression variableExpression) {
        return variableExpression.variable.getDataType(environment);
    }


    @Override
    public Type visit(NamedVariable namedVariable) {
        VariableEntry variableEntry = environment.getVariable(namedVariable.name)
                .orElseThrow(() -> new ErrorMessage.Undefined("Variable", namedVariable.name));

        return variableEntry.type;
    }

    @Override
    public Type visit(IndexedVariable indexedVariable) {
        VariableEntry variableEntry = environment.getVariable(indexedVariable.name)
                .orElseThrow(() -> new ErrorMessage.Undefined("Variable", indexedVariable.name));

        if (!(variableEntry.type instanceof ArrayType)) {
            throw new ErrorMessage.IndexingNonArray(indexedVariable.name, variableEntry.type);
        }

        Type indexType = indexedVariable.index.getDataType(environment);
        if (indexType != PrimitiveType.IntType) {
            throw new ErrorMessage.IndexingWithNonInteger(indexedVariable.index.position, indexType);
        }

        // Arrays consist only of integers, so this element must be an integer.
        return PrimitiveType.IntType;
    }
}
