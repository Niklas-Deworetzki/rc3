package rc3.januscompiler.backend.optimizer;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.JanusPrettyPrinter;
import rc3.januscompiler.pass.Pass;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.table.Environment;
import rc3.lib.dataflow.ConstantLattice;
import rc3.lib.optimization.Optimization;
import rc3.lib.optimization.OptimizationState;
import rc3.lib.parsing.Position;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static rc3.januscompiler.syntax.AssignStatement.ModificationOperator;
import static rc3.lib.optimization.AvailableOptimization.LOOP_OPTIMIZATION;

/**
 * Perform loop unrolling on Janus programs.
 * <p>
 * Every {@link ProcedureDeclaration procedure} of a Janus program is scanned for
 * loops with constant iteration counts. This is done using a <i>depth first</i>
 * approach, analyzing the most-nested loops first. If the iteration count of
 * a loop does not exceed a certain threshold, the loop is unrolled. Unrolling
 * creates multiple copies of the loop bodies according to the iteration count,
 * removes the conditions that would be checked during every iteration, replaces
 * the loop variable with a constant in every occurrence within these bodies and
 * sets the loop variable to the correct value afterwards.
 * <p>
 * In order to unroll a loop, some conditions have to be met:
 * <ol>
 *  <li>The {@link LoopStatement#fromCondition from condition} compares
 *  that a variable <var>x</var> is a constant value.</li>
 *
 *  <li>The {@link LoopStatement#untilCondition until condition} compares
 *  that a variable <var>y</var> is a constant value.</li>
 *
 *  <li>Both variables <var>x</var> and <var>y</var> are the same
 *  {@link NamedVariable}. This is the <i>loop variable</i>.</li>
 *
 *  <li>All changes to the loop variable are increments or decrements with a
 *  constant value. Otherwise the optimizer won't be able to predict, how
 *  often the loop is executed.</li>
 *
 *  <li>It is possible to calculate the iteration count of the loop from the
 *  loop variables initial value, final value and both increments.</li>
 *
 *  <li>The iteration count of the loop does not exceed
 *  {@link LoopOptimizerPass#maximumIterationCount()}</li>
 * </ol>
 * <p>
 * In order to calculate the changes made to the loop variable within one of
 * the loop's bodies, all changes to the variable within the body are summed.
 * If any of the changes are unknown, this is represented using
 * {@link ConstantLattice#bottom()} and the total increment becomes unknown
 * as well. If no change is made, this is represented using
 * {@link ConstantLattice#top()} and later interpreted as a change of <i>+0</i>.
 * Other changes are represented using their {@link ConstantLattice#lift(Object)
 * numerical value}. The change made to the loop variable depends on each statement's
 * variant and is determined by {@link LoopOptimizerPass#getIncrement(NamedVariable, Statement, Map)}.
 * The last parameter allows {@link AssignStatement}s to enter their increment. This
 * is later used when unrolling a loop. {@link AssignStatement}s stored within the
 * {@link Map} are not emitted as part of an unrolled loops body. Instead, the increment
 * stored for them is added to the loop variable's value.
 * <ul>
 *  <li>{@link AssignStatement}s that modify the loop variable, have a constant value
 *  and do <b>not</b> use {@link ModificationOperator#XOR} as a modification operator,
 *  have the constant value marked as the increment. If the loop variable is modified,
 *  but the {@link ModificationOperator#XOR} operator is used or the value is not a
 *  constant, the increment is reported as unknown. If the statement does not change
 *  the loop variable, no increment is reported.</li>
 *
 *  <li>{@link BuiltinStatement} and {@link CallStatement} report an unknown increment,
 *  if the loop variable appears as one of the arguments. Otherwise, no increment is
 *  reported.</li>
 *
 *  <li>{@link SwapStatement}s report an unknown increment, if one of the variables is
 *  the loop variable. Otherwise, no increment is reported.</li>
 *
 *  <li>{@link SkipStatement}s report no increment.</li>
 *
 *  <li>{@link LocalBlockStatement}s report their {@link LocalBlockStatement#body bodies}
 *  increment as their increment.</li>
 *
 *  <li>{@link IfStatement}s report the increment of their branches, if both branches
 *  report <b>the same</b> increment (regardless if it is no increment, a constant increment
 *  or an unknown increment). Otherwise, an unknown increment is reported.</li>
 *
 *  <li>{@link LoopStatement}s report an unknown increment, if any of their bodies reports
 *  an increment. If both bodies report no increment, the loop in total reports no increment
 *  as well. Otherwise, the increment is unknown, as the loop's iteration count is unknown
 *  as well.</li>
 * </ul>
 */
public class LoopOptimizerPass extends Optimization implements Pass {
    private static final ConstantLattice<Integer> LATTICE = ConstantLattice.instance();

    public static final int MAXIMUM_ITERATION_COUNT = 20;
    private final int maximumIterationCount;

    // Filled during analysis when loops are not optimized but have a constant iteration count.
    private final Map<LoopStatement, LoopInfo> knownLoops = new HashMap<>();

    public LoopOptimizerPass(OptimizationState optimizationState) {
        super(LOOP_OPTIMIZATION, optimizationState);

        int iterationCount = MAXIMUM_ITERATION_COUNT;
        for (String optimizationParameter : getOptimizationParameters()) {
            try {
                iterationCount = Integer.parseInt(optimizationParameter);
            } catch (NumberFormatException illegalArgument) {
                debug("Illegal parameter '%s' for loop unrolling is not a number.", iterationCount);
            }
        }
        this.maximumIterationCount = iterationCount;
        debug("Maximum iteration count unrolled is %d.", this.maximumIterationCount);
    }

    private int maximumIterationCount() {
        return maximumIterationCount;
    }

    @Override
    public void execute(Compiler compiler, Environment environment, Program program) {
        if (isEnabled()) {
            program.procedures.forEach(this::optimizeProcedure);
        }
    }

    /**
     * Replaces the {@link ProcedureDeclaration#body body of a procedure}
     * with an optimized statement list.
     */
    public void optimizeProcedure(ProcedureDeclaration procedure) {
        knownLoops.clear();

        procedure.body = optimizeStatementList(procedure.body);
        debug(() -> new JanusPrettyPrinter().visit(procedure));
    }

    /**
     * Depth-first approach to optimize statements. All {@link LoopStatement}s
     * found are attempted to be optimized.
     *
     * @see LoopOptimizerPass#attemptLoopUnrolling(LoopStatement)
     */
    private List<Statement> optimizeStatementList(List<Statement> statementList) {
        return flatmapStatementList(statementList, statement -> switch (statement) {
            case LoopStatement loopStatement -> attemptLoopUnrolling(loopStatement);

            case IfStatement ifStatement -> List.of(
                    new IfStatement(ifStatement.position, ifStatement.condition,
                            optimizeStatementList(ifStatement.thenStatements),
                            optimizeStatementList(ifStatement.elseStatements),
                            ifStatement.assertion));

            case LocalBlockStatement local -> List.of(
                    new LocalBlockStatement(local.position,
                            local.localDeclaration, local.localInitializer,
                            optimizeStatementList(local.body),
                            local.delocalDeclaration, local.delocalInitializer));

            case SkipStatement ignored -> Collections.emptyList();

            default -> List.of(statement);
        }, true);
    }

    private List<Statement> attemptLoopUnrolling(LoopStatement loopStatement) {
        // Proceed with depth-first approach, optimizing both bodies first.
        final List<Statement> optDoStatements = optimizeStatementList(loopStatement.doStatements);
        final List<Statement> optLoopStatements = optimizeStatementList(loopStatement.loopStatements);
        final Supplier<LoopStatement> fallbackValue = () -> // Construct this result only once and on demand.
                new LoopStatement(loopStatement.position, loopStatement.fromCondition,
                        optDoStatements, optLoopStatements, loopStatement.untilCondition);

        // Infer and verify loop bounds.
        final var upperBound = LoopBound.fromExpression(loopStatement.fromCondition);
        final var lowerBound = LoopBound.fromExpression(loopStatement.untilCondition);
        if (upperBound.isEmpty() || lowerBound.isEmpty() ||
                !hasEqualName(upperBound.get().variable(), lowerBound.get().variable())) {
            // Loop bounds are not constant or have different variables.
            return List.of(fallbackValue.get());
        }

        final NamedVariable loopVariable = upperBound.get().variable();

        // Get increments for both bodies.
        final Map<AssignStatement, Integer> modifyingStatements = new HashMap<>();
        final var doIncrement = getIncrement(loopVariable, optDoStatements, modifyingStatements);
        final var loopIncrement = getIncrement(loopVariable, optLoopStatements, modifyingStatements);
        if (doIncrement.isBottom() || loopIncrement.isBottom()) {
            debug("Loop with variable %s cannot be optimized, as increment is unknown.", loopVariable.name);
            // Loop bodies perform unknown increments.
            return List.of(fallbackValue.get());
        }

        final LoopInfo info = new LoopInfo(loopVariable, upperBound.get().value, lowerBound.get().value,
                doIncrement.get().orElse(0), loopIncrement.get().orElse(0));

        // Check how many times the loop will be unrolled.
        final int iterationCount = info.getIterationCount();
        if (iterationCount < 0) { // Unrolling will never finish.
            debug("Loop with %s has unknown iteration count.", info);
            return List.of(fallbackValue.get());
        } else if (iterationCount > maximumIterationCount()) { // Unrolling exceeds limit.
            debug("Loop with %s exceeds unrolling limits of %d+1.", info, maximumIterationCount());
            final LoopStatement returnedLoop = fallbackValue.get();

            knownLoops.put(returnedLoop, info); // Remember iteration count for later.
            return List.of(returnedLoop);
        }

        debug("Loop with %s will be unrolled %d+1 times.", info, iterationCount);
        return new LoopUnroller(info,
                loopStatement.doStatements, loopStatement.loopStatements,
                modifyingStatements, iterationCount).unroll();
    }


    /**
     * Data class holding the loop variable, its initial value, its final value and the constant
     * increments for both bodies.
     */
    public record LoopInfo(NamedVariable variable, int initialValue, int finalValue,
                           int doIncrement, int loopIncrement) {
        /**
         * Attempts to calculate how many times a loop with this information would be executed.
         * The number returned is the iteration count in addition to the first iteration of the
         * {@link LoopStatement#doStatements do body}, as this is always executed.
         * <p>
         * A negative result indicates that the loop will never terminate or that the iteration
         * count cannot be computed.
         */
        public int getIterationCount() {
            if (finalValue - initialValue == doIncrement) {
                return 0; // Special case, if no additional iteration is performed.
            }

            final int valueAfterFirstIteration = initialValue + doIncrement;
            final int iterationIncrement = loopIncrement + doIncrement;
            final int difference = finalValue - valueAfterFirstIteration;

            if (iterationIncrement != 0 && // Iteration changes the value of the loop variable.
                    (iterationIncrement > 0 == difference > 0) && // Change has the same sign as difference.
                    difference % iterationIncrement == 0) { // Difference is a multiple of change.
                return difference / iterationIncrement;
            }
            return -1;
        }

        @Override
        public String toString() {
            return String.format("variable %s from %d to %d (increment %d,%d)",
                    variable.name, initialValue, finalValue, doIncrement, loopIncrement);
        }
    }


    /**
     * Data class used to represent a loop bound. A bound is a condition verifying that
     * a {@link NamedVariable} has a constant value.
     */
    private record LoopBound(NamedVariable variable, int value) {
        /**
         * Tries to extract a {@link LoopBound} from an equation. This method already assumes that
         * a {@link VariableExpression} is compared to another {@link Expression}.
         * If returns a result, if the {@link VariableExpression} is a {@link NamedVariable} and
         * the {@link Expression} it is compared to for equality has a constant value.
         */
        private static Optional<LoopBound> fromEquation(VariableExpression variable, Expression expression) {
            if (variable.variable instanceof NamedVariable namedVariable) {
                final var value = StaticEvaluator.valueOf(expression);
                if (value.isPresent()) {
                    return Optional.of(new LoopBound(namedVariable, value.getAsInt()));
                }
            }
            return Optional.empty();
        }

        /**
         * Tries to extract a {@link LoopBound} from an {@link Expression}.
         * <p>
         * If the Expression has the form <i>Variable = Constant</i>, this
         * function returns a value. This method also handles the commutativity
         * of the equality operator and allows <i>Constant = Variable</i> as well.
         */
        public static Optional<LoopBound> fromExpression(Expression expression) {
            if (expression instanceof BinaryExpression binaryExpression &&
                    binaryExpression.operator == BinaryExpression.BinaryOperator.EQU) {

                if (binaryExpression.lhs instanceof VariableExpression variable) {
                    return fromEquation(variable, binaryExpression.rhs);

                } else if (binaryExpression.rhs instanceof VariableExpression variable) {
                    return fromEquation(variable, binaryExpression.lhs);

                }
            }
            return Optional.empty();
        }
    }


    /**
     * Computes the sum of all increments for the {@link Statement}s in the given {@link List}.
     */
    private ConstantLattice.Value<Integer> getIncrement(NamedVariable loopVariable, List<Statement> statementList,
                                                        Map<AssignStatement, Integer> modifyingStatements) {
        ConstantLattice.Value<Integer> totalIncrement = LATTICE.top();
        for (Statement statement : statementList) {
            final var localIncrement = getIncrement(loopVariable, statement, modifyingStatements);
            if (localIncrement.isPresent()) {
                if (totalIncrement.isTop()) {
                    // If this is the first change, use it as total change.
                    totalIncrement = localIncrement;
                } else {
                    // Otherwise add both changes together.
                    totalIncrement = LATTICE.lift(totalIncrement.unpack() + localIncrement.unpack());
                }
            } else if (localIncrement.isBottom()) {
                return LATTICE.bottom(); // Abort once the change becomes unknown.
            }
        }
        return totalIncrement;
    }

    /**
     * Detects the constant increment of a {@link Statement} to the loop variable.
     * {@link AssignStatement}s with a constant increment are added to the {@link Map}.
     */
    private ConstantLattice.Value<Integer> getIncrement(NamedVariable loopVariable, Statement statement,
                                                        Map<AssignStatement, Integer> modifyingStatements) {
        return switch (statement) {
            case AssignStatement assignStatement -> {
                if (hasEqualName(loopVariable, assignStatement.variable)) {
                    // Change to loop variable is made. Determine increment.
                    final var increment = StaticEvaluator.valueOf(assignStatement.value);
                    if (increment.isPresent() && assignStatement.modificationOperator != ModificationOperator.XOR) {
                        int value = increment.getAsInt();
                        if (assignStatement.modificationOperator == ModificationOperator.SUB) {
                            value = -value;
                        }

                        modifyingStatements.put(assignStatement, value); // Remember incrementing statement for later.
                        yield LATTICE.lift(value);

                    } else {
                        // Loop variable is changed by an unknown amount.
                        yield LATTICE.bottom();
                    }
                }
                yield LATTICE.top();
            }

            case BuiltinStatement builtinStatement -> {
                for (Variable variable : builtinStatement.argumentList) {
                    if (hasEqualName(loopVariable, variable)) yield LATTICE.bottom();
                }
                yield LATTICE.top();
            }

            case CallStatement callStatement -> {
                for (Variable variable : callStatement.argumentList) {
                    if (hasEqualName(loopVariable, variable)) yield LATTICE.bottom();
                }
                yield LATTICE.top();
            }

            case IfStatement ifStatement -> {
                final var thenIncrement = getIncrement(loopVariable, ifStatement.thenStatements, modifyingStatements);
                final var elseIncrement = getIncrement(loopVariable, ifStatement.elseStatements, modifyingStatements);
                if (thenIncrement.equals(elseIncrement)) yield thenIncrement; // Both changes are equal.
                yield LATTICE.bottom(); // Otherwise, the total change is unknown.
            }

            case LocalBlockStatement localBlockStatement ->
                    (localBlockStatement.localDeclaration.name.equals(loopVariable.name))
                            ? LATTICE.top() // Loop variable is shadowed and cannot be modified.
                            : getIncrement(loopVariable, localBlockStatement.body, modifyingStatements);

            case LoopStatement loopStatement -> {
                final var doIncrement = getIncrement(loopVariable, loopStatement.doStatements, modifyingStatements);
                final var loopIncrement = getIncrement(loopVariable, loopStatement.loopStatements, modifyingStatements);
                if (doIncrement.isTop() && loopIncrement.isTop()) { // No change in loop.
                    yield LATTICE.top();
                } else if (doIncrement.isBottom() || loopIncrement.isBottom() || // Change is unknown
                        !knownLoops.containsKey(loopStatement)) { // or number of iterations is unknown.
                    yield LATTICE.bottom();
                } else {
                    final int doValue = doIncrement.get().orElse(0);
                    final int loopValue = loopIncrement.get().orElse(0);
                    final LoopInfo info = knownLoops.get(loopStatement);

                    yield LATTICE.lift(doValue + info.getIterationCount() * (doValue + loopValue));
                }
            }

            case SkipStatement ignored -> LATTICE.top();

            case SwapStatement swapStatement -> {
                if (hasEqualName(loopVariable, swapStatement.lhsvar) ^
                        hasEqualName(loopVariable, swapStatement.rhsvar)) {
                    yield LATTICE.bottom();
                }
                yield LATTICE.top();
            }
        };
    }


    /**
     * Applies the given {@link Function} to every {@link Statement} in the given {@link List
     * statement list} in order to build a single result {@link List statement list}.
     * <p>
     * Optionally, if the result would be an empty {@link List}, a singular {@link SkipStatement}
     * is added to ensure consistency with Janus' syntax rules.
     *
     * @param statementList            The {@link List} of {@link Statement}s to which the
     *                                 {@link Function} is applied to.
     * @param function                 The {@link Function} applied to every {@link Statement}.
     * @param addDummyStatementIfEmpty If <code>true</code>, a single {@link SkipStatement}
     *                                 is added to the result, if it would otherwise be empty.
     */
    static List<Statement> flatmapStatementList(List<Statement> statementList,
                                                Function<Statement, List<Statement>> function,
                                                boolean addDummyStatementIfEmpty) {
        final List<Statement> result = new ArrayList<>();
        for (Statement statement : statementList) {
            result.addAll(function.apply(statement));
        }
        if (addDummyStatementIfEmpty && result.isEmpty()) {
            result.add(new SkipStatement(Position.NONE));
        }
        return result;
    }

    /**
     * Utility function used to check whether a {@link Variable} is equal to a {@link NamedVariable}.
     *
     * @param named    A known {@link NamedVariable}, usually the loop variable.
     * @param variable An arbitrary {@link Variable}.
     * @return <code>true</code> if the second {@link Variable} is a {@link NamedVariable}
     * with the same name as the first one.
     */
    static boolean hasEqualName(NamedVariable named, Variable variable) {
        return variable instanceof NamedVariable other && named.name.equals(other.name);
    }
}
