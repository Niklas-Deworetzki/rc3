package rc3.januscompiler.backend.optimizer;

import rc3.januscompiler.syntax.*;
import rc3.lib.parsing.Position;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static rc3.januscompiler.backend.optimizer.LoopOptimizerPass.flatmapStatementList;
import static rc3.januscompiler.backend.optimizer.LoopOptimizerPass.hasEqualName;

final class LoopUnroller {
    private final LoopOptimizerPass.LoopInfo info;
    private final List<Statement> doBody, loopBody;
    private final Map<AssignStatement, Integer> modifyingStatements;
    private final int iterationCount;

    private int loopVariableValue;


    public LoopUnroller(LoopOptimizerPass.LoopInfo info,
                        List<Statement> doBody, List<Statement> loopBody,
                        Map<AssignStatement, Integer> modifyingStatements, int iterationCount) {
        this.info = info;
        this.doBody = doBody;
        this.loopBody = loopBody;
        this.modifyingStatements = modifyingStatements;
        this.iterationCount = iterationCount;
    }

    public List<Statement> unroll() {
        // Initialize value of loop variable.
        loopVariableValue = info.initialValue();

        // Unroll loop.
        final List<Statement> unrolledStatements = unrollBody(doBody);
        for (int iteration = 0; iteration < iterationCount; iteration++) {
            unrolledStatements.addAll(unrollBody(loopBody));
            unrolledStatements.addAll(unrollBody(doBody));
        }

        // Correct value of loop variable: variable += (final - initial)
        unrolledStatements.add(new AssignStatement(Position.NONE,
                info.variable(), AssignStatement.ModificationOperator.ADD,
                new IntLiteral(Position.NONE, info.finalValue() - info.initialValue())));

        assert loopVariableValue == info.finalValue();
        return unrolledStatements;
    }


    private List<Statement> unrollBody(List<Statement> body) {
        return flatmapStatementList(body, this::replaceLoopVariable, false);
    }


    private List<Statement> replaceLoopVariable(Statement statement) {
        return switch (statement) {
            case AssignStatement assignStatement -> {
                if (modifyingStatements.containsKey(assignStatement)) {
                    loopVariableValue += modifyingStatements.get(assignStatement);
                    yield emptyList();
                }

                yield List.of(new AssignStatement(statement.position,
                        replaceLoopVariable(assignStatement.variable),
                        assignStatement.modificationOperator,
                        replaceLoopVariable(assignStatement.value)));
            }

            case IfStatement ifStatement -> {
                final int valueAtBegin = loopVariableValue;
                final var thenStatements = flatmapStatementList(ifStatement.thenStatements, this::replaceLoopVariable, true);
                loopVariableValue = valueAtBegin;
                final var elseStatements = flatmapStatementList(ifStatement.elseStatements, this::replaceLoopVariable, true);

                yield List.of(new IfStatement(statement.position,
                        replaceLoopVariable(ifStatement.condition),
                        thenStatements,
                        elseStatements,
                        replaceLoopVariable(ifStatement.assertion)));
            }

            case BuiltinStatement builtinStatement -> List.of(new BuiltinStatement(statement.position,
                    builtinStatement.builtinProcedure,
                    builtinStatement.argumentList.stream().map(this::replaceLoopVariable).toList()));
            case CallStatement callStatement -> List.of(new CallStatement(statement.position,
                    callStatement.direction,
                    callStatement.procedureName,
                    callStatement.argumentList.stream().map(this::replaceLoopVariable).toList()));
            case LocalBlockStatement localBlockStatement -> {
                if (localBlockStatement.localDeclaration.name.equals(info.variable().name))
                    yield List.of(localBlockStatement);

                yield List.of(new LocalBlockStatement(statement.position,
                        localBlockStatement.localDeclaration,
                        replaceLoopVariable(localBlockStatement.localInitializer),
                        flatmapStatementList(localBlockStatement.body, this::replaceLoopVariable, true),
                        localBlockStatement.delocalDeclaration,
                        replaceLoopVariable(localBlockStatement.delocalInitializer)));
            }
            case LoopStatement loopStatement -> List.of(new LoopStatement(statement.position,
                    replaceLoopVariable(loopStatement.fromCondition),
                    flatmapStatementList(loopStatement.doStatements, this::replaceLoopVariable, true),
                    flatmapStatementList(loopStatement.loopStatements, this::replaceLoopVariable, true),
                    replaceLoopVariable(loopStatement.untilCondition)));
            case SkipStatement ignored -> emptyList();
            case SwapStatement swapStatement -> List.of(new SwapStatement(statement.position,
                    replaceLoopVariable(swapStatement.lhsvar),
                    replaceLoopVariable(swapStatement.rhsvar)));
        };
    }

    private Expression replaceLoopVariable(Expression expression) {
        return switch (expression) {
            case BinaryExpression binaryExpression -> new BinaryExpression(expression.position,
                    replaceLoopVariable(binaryExpression.lhs),
                    binaryExpression.operator,
                    replaceLoopVariable(binaryExpression.rhs));
            case BuiltinExpression builtinExpression -> new BuiltinExpression(expression.position,
                    builtinExpression.expression,
                    builtinExpression.argumentList.stream().map(this::replaceLoopVariable).toList());
            case IntLiteral intLiteral -> intLiteral;
            case StackNilLiteral stackNilLiteral -> stackNilLiteral;
            case VariableExpression variableExpression -> {
                if (hasEqualName(info.variable(), variableExpression.variable))
                    yield new IntLiteral(expression.position, loopVariableValue);
                else
                    yield new VariableExpression(expression.position,
                            replaceLoopVariable(variableExpression.variable));
            }
        };
    }

    private Variable replaceLoopVariable(Variable variable) {
        return switch (variable) {
            case IndexedVariable indexedVariable -> new IndexedVariable(variable.position,
                    indexedVariable.name, replaceLoopVariable(indexedVariable.index));
            case NamedVariable namedVariable -> namedVariable;
        };
    }
}
