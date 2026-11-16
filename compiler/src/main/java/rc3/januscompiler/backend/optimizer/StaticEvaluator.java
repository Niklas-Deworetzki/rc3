package rc3.januscompiler.backend.optimizer;

import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;

public final class StaticEvaluator extends DoNothingTreeVisitor<OptionalInt> {
    private static final StaticEvaluator INSTANCE = new StaticEvaluator();

    public static Optional<Integer> boxedValueOf(Expression expression) {
        final var value = valueOf(expression);
        if (value.isPresent()) return Optional.of(value.getAsInt());
        return Optional.empty();
    }

    public static OptionalInt valueOf(Expression expression) {
        try {
            return expression.accept(INSTANCE);
        } catch (ArithmeticException ignored) {
            return OptionalInt.empty();
        }
    }

    private StaticEvaluator() {
        super(OptionalInt.empty());
    }

    private static final Map<BinaryExpression.BinaryOperator, IntBinaryOperator> JAVA_OPERATORS = Map.of(
            BinaryExpression.BinaryOperator.ADD, (x, y) -> x + y,
            BinaryExpression.BinaryOperator.SUB, (x, y) -> x - y,
            BinaryExpression.BinaryOperator.MUL, (x, y) -> x * y,
            BinaryExpression.BinaryOperator.DIV, (x, y) -> x / y,
            BinaryExpression.BinaryOperator.MOD, (x, y) -> x % y,
            BinaryExpression.BinaryOperator.AND, (x, y) -> x & y,
            BinaryExpression.BinaryOperator.XOR, (x, y) -> x ^ y,
            BinaryExpression.BinaryOperator.OR, (x, y) -> x | y
    );

    @Override
    public OptionalInt visit(BinaryExpression binaryExpression) {
        final var lhsVal = binaryExpression.lhs.accept(this);
        final var rhsVal = binaryExpression.rhs.accept(this);
        final var operator = JAVA_OPERATORS.get(binaryExpression.operator);

        if (lhsVal.isPresent() && rhsVal.isPresent() && operator != null) {
            return OptionalInt.of(operator.applyAsInt(lhsVal.getAsInt(), rhsVal.getAsInt()));
        }

        return OptionalInt.empty();
    }

    @Override
    public OptionalInt visit(IntLiteral intLiteral) {
        return OptionalInt.of(intLiteral.value);
    }
}
