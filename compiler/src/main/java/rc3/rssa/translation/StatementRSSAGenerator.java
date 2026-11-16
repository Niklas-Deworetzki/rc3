package rc3.rssa.translation;

import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.types.PrimitiveType;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.instances.Variable;
import rc3.rssa.instances.*;

import java.util.*;

/**
 * An <i>abstract</i> generator class used to translate {@link Statement Statements} into RSSA-Instructions.
 * <p>
 * This abstract class implements the visitor pattern for the {@link TreeVisitor}
 * interface, providing <code>visit</code> methods for all {@link Statement} instances except
 * <code>visit({@link LoopStatement})</code> and <code>visit({@link IfStatement})</code>.
 * Those two methods are provided by one of the concrete implementations, implementing different approaches to
 * the translation of conditionals.
 * </p>
 *
 * @see AssigningConditionalGenerator
 * @see ReversingConditionalGenerator
 */
public abstract class StatementRSSAGenerator extends DoNothingVoidTreeVisitor {
    protected final RSSAGenerator generator;

    public StatementRSSAGenerator(RSSAGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void visitVoid(AssignStatement assignStatement) {
        Value value = generator.translateExpression(assignStatement.value);

        if (assignStatement.variable instanceof IndexedVariable) {
            // M[ Ass.Id.Index ] ±= E [ Ass.Val ]
            MemoryAccess variable = generator.fromIndexedVariable((IndexedVariable) assignStatement.variable);
            generator.emit(new MemoryAssignment(variable, assignStatement.modificationOperator,
                    generator.promoteForMemoryAssignment(value)));

        } else {
            // Ass.Id := Ass.Id ± E [ Ass.Val ]
            Variable variable = generator.fromNamedVariable((NamedVariable) assignStatement.variable);
            generator.emit(new ArithmeticAssignment(variable, variable, assignStatement.modificationOperator,
                    generator.promoteForArithmeticAssignment(value)));
        }

        // Destroy
        generator.emitPendingDestruction();
    }

    @Override
    public void visitVoid(BuiltinStatement builtinStatement) {
        switch (builtinStatement.builtinProcedure) {
            case STACK_POP -> {
                StackVariable stackVariable = new StackVariable(generator,
                        builtinStatement.argumentList.get(1).name);

                stackVariable.pop(builtinStatement.argumentList.get(0));
            }
            case STACK_PUSH -> {
                StackVariable stackVariable = new StackVariable(generator,
                        builtinStatement.argumentList.get(1).name);

                stackVariable.push(builtinStatement.argumentList.get(0));
            }
            default -> throw new ErrorMessage.UnsupportedOperation("RSSA does not support the builtin " +
                    builtinStatement.builtinProcedure.getName() + " statement!", builtinStatement.position);
        }
    }

    @Override
    public void visitVoid(CallStatement callStatement) {
        // (arg1,arg2,...) := CallStm.Direction CallStm.Procedure (arg1,arg2,...)

        Queue<Variable> providedArguments = new ArrayDeque<>(callStatement.argumentList.size());

        // Add a Variable to providedArguments for every parameter passed.
        for (var argument : callStatement.argumentList) {
            if (argument instanceof IndexedVariable) {
                // Bind the value of the IndexedVariable to a temporary name.
                MemoryAccess argInMemory = generator.fromIndexedVariable((IndexedVariable) argument);
                Variable loadedArg = Variable.nextTemporaryVariable();

                // Arg := M [ Arg.Index ] := 0 (Load from memory)
                generator.emit(new MemoryInterchangeInstruction(
                        loadedArg, argInMemory, new Constant(0)));

                // Cleanup (index calculation may build up temporaries).
                generator.emitPendingDestruction();

                providedArguments.add(loadedArg);


            } else if (argument.dataType == PrimitiveType.StackType) {
                StackVariable stackVariable = new StackVariable(generator, argument.name);

                providedArguments.add(stackVariable.getStackReference());
                providedArguments.add(stackVariable.getStackTop());

            } else {
                providedArguments.add(generator.fromNamedVariable((NamedVariable) argument));
            }
        }

        generator.emit(new CallInstruction(
                new ArrayList<>(providedArguments),
                callStatement.direction,
                callStatement.procedureName.toString(),
                new ArrayList<>(providedArguments)));

        // Update values that were stored in an array.
        for (var argument : callStatement.argumentList) {
            Variable providedName = providedArguments.remove();
            if (argument instanceof IndexedVariable) {
                MemoryAccess argInMemory = generator.fromIndexedVariable((IndexedVariable) argument);

                // 0 := M [ Arg.Index ] := Arg (Store in memory)
                generator.emit(new MemoryInterchangeInstruction(
                        new Constant(0), argInMemory, providedName));

                generator.emitPendingDestruction();

            } else if (argument.dataType == PrimitiveType.StackType) {
                // Remove the additional name from the queue.
                providedArguments.remove();
            }
        }
    }

    @Override
    public void visitVoid(LocalBlockStatement localBlockStatement) {
        generator.getEnvironment().enter(localBlockStatement.localDeclaration.entry);

        boolean isStack = localBlockStatement.localDeclaration.type == PrimitiveType.StackType;
        if (isStack) { // Allocate local stack.
            StackVariable stack = new StackVariable(generator, localBlockStatement.localDeclaration.name);

            if (localBlockStatement.localInitializer instanceof VariableExpression) {
                Identifier otherStackName = ((VariableExpression) localBlockStatement.localInitializer).variable.name;
                StackVariable other = new StackVariable(generator, otherStackName);

                stack.copyFrom(other);
            } else if (localBlockStatement.localInitializer instanceof StackNilLiteral) {
                stack.alloc();
            }

        } else {
            Variable variable = generator.fromName(localBlockStatement.localDeclaration.name);

            // Local.Id := 0 ^ E [ Local.Init ]
            Value localExpression = generator.translateExpression(localBlockStatement.localInitializer);
            generator.emit(ArithmeticAssignment.assignWithoutDestroy(variable,
                    generator.promoteForArithmeticAssignment(localExpression)));
            generator.emitPendingDestruction();
        }

        {
            // E [ Local.Body ]
            localBlockStatement.body.forEach(generator::translateStatement);
        }

        if (isStack) { // Free local stack.
            StackVariable stack = new StackVariable(generator, localBlockStatement.delocalDeclaration.name);

            if (localBlockStatement.delocalInitializer instanceof VariableExpression) {
                Identifier otherStackName = ((VariableExpression) localBlockStatement.delocalInitializer).variable.name;
                StackVariable other = new StackVariable(generator, otherStackName);

                stack.destroyWith(other);
            } else if (localBlockStatement.delocalInitializer instanceof StackNilLiteral) {
                stack.free();
            }


        } else {
            Variable variable = generator.fromName(localBlockStatement.delocalDeclaration.name);

            // 0 := Local.Id ^ E [ Local.DeInit ]
            Value delocalExpression = generator.translateExpression(localBlockStatement.delocalInitializer);
            generator.emit(new ArithmeticAssignment(
                    new Constant(0), variable, AssignStatement.ModificationOperator.XOR,
                    generator.promoteForArithmeticAssignment(delocalExpression)));
            generator.emitPendingDestruction();
        }

        generator.getEnvironment().exit(localBlockStatement.delocalDeclaration.entry);
    }


    @Override
    public void visitVoid(SkipStatement skipStatement) {
        // Nothing to do!
    }

    @Override
    public void visitVoid(SwapStatement swapStatement) {
        boolean leftIsArray = swapStatement.lhsvar instanceof IndexedVariable;
        boolean rightIsArray = swapStatement.rhsvar instanceof IndexedVariable;

        if (!leftIsArray && !rightIsArray) {
            Variable left = generator.fromNamedVariable((NamedVariable) swapStatement.lhsvar);
            Variable right = generator.fromNamedVariable((NamedVariable) swapStatement.rhsvar);

            // Left,Right <-> Right,Left
            generator.emit(new SwapInstruction(left, right, right, left));

        } else if (leftIsArray && rightIsArray) {
            // M[l] := E [ Swap.Lhs ]
            MemoryAccess left = generator.fromIndexedVariable((IndexedVariable) swapStatement.lhsvar);
            // M[r] := E [ Swap.Rhs ]
            MemoryAccess right = generator.fromIndexedVariable((IndexedVariable) swapStatement.rhsvar);

            // M[l] <-> M[r]
            generator.emit(new MemorySwapInstruction(left, right));

        } else {
            NamedVariable simple;
            IndexedVariable array;
            if (leftIsArray) {
                array = (IndexedVariable) swapStatement.lhsvar;
                simple = (NamedVariable) swapStatement.rhsvar;
            } else {
                array = (IndexedVariable) swapStatement.rhsvar;
                simple = (NamedVariable) swapStatement.lhsvar;
            }

            // Atom := M[Idx] := Atom
            MemoryAccess memoryVariable = generator.fromIndexedVariable(array);
            Variable stackVariable = generator.fromNamedVariable(simple);
            generator.emit(new MemoryInterchangeInstruction(
                    stackVariable, memoryVariable, stackVariable));
        }
        // Cleanup temporaries that may have been created during index calculation.
        generator.emitPendingDestruction();
    }

    /**
     * This concrete subclass of {@link StatementRSSAGenerator} implements control flow operation using primitive
     * evaluation and assignment to temporary variables.
     * <p>
     * If an {@link Expression} <var>E</var> is used as an condition to select between multiple jumping targets,
     * following code will be generated by this {@link StatementRSSAGenerator}:
     * <ol>
     *  <li><var>E</var> is evaluated into a {@link Value} <var>R</var> holding the result.</li>
     *  <li><var>R</var> is bound to a temporary {@link Variable} <var>T</var> without destroying <var>R</var>.</li>
     *  <li><var>E</var> is explicitly un-computed, cleaning up all used resources except <var>T</var>.</li>
     * </ol>
     * </p>
     * <p>
     * The temporary variable <var>T</var> can then be used to select the jumping target as following:
     * <pre>
     *  T != 0 -> L1 (T) L2
     * </pre>
     * This essentially implements an <i>is-true</i> check to access the boolean result stored in <var>T</var>.
     * </p>
     * <p>
     * To clean up and destroy <var>T</var>, the scheme from above is repeated in both possible branches:
     * <ol>
     *  <li><var>E</var> is evaluated again into a {@link Value} <var>R</var> holding the result.</li>
     *  <li><var>T</var> is destroyed using <var>R</var>.</li>
     *  <li><var>E</var> is explicitly un-computed again, cleaning up all remaining resources.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Advantages</b> of this implementation include the easy implementation as part of the generator, since no
     * additional logic is needed, to track and clean up temporary variables.
     * It is only necessary to use the same name for <var>T</var> when creating and cleaning up the expression.
     * </p>
     * <p>
     * An obvious <b>disadvantage</b> of this method is the high computing power <i>"wasted"</i> during repeated
     * computation of <var>R</var>. Additionally the code size is increased, since instructions on how to evaluate
     * <var>E</var> to <var>R</var> are emitted a total of six times.
     * </p>
     */
    public static final class AssigningConditionalGenerator extends StatementRSSAGenerator {

        public AssigningConditionalGenerator(RSSAGenerator generator) {
            super(generator);
        }

        private void generateBranch(String labelEntry, String labelExit, List<Statement> branchStatements,
                                    Variable conditionVariable, Expression condition,
                                    Variable assertionVariable, Expression assertion) {
            // Entry() <-
            generator.emit(new UnconditionalEntry(labelEntry));

            // 0 := Tc ^ E [ Condition ]
            Value conditionValue = generator.translateExpression(condition);
            generator.emit(ArithmeticAssignment.assignWithoutDestroy(
                            conditionVariable, generator.promoteForConditional(conditionValue))
                    .reverse());
            generator.emitPendingDestruction();

            // E [ Branch ]
            branchStatements.forEach(generator::translateStatement);

            // Ta := 0 ^ E [ Assertion ]
            Value assertionValue = generator.translateExpression(assertion);
            generator.emit(ArithmeticAssignment.assignWithoutDestroy(
                    assertionVariable, generator.promoteForArithmeticAssignment(assertionValue)));
            generator.emitPendingDestruction();

            // -> Exit(Ta)
            generator.emitWithParameters(new UnconditionalExit(labelExit), assertionVariable);
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            // Prepare names used to implement control flow.
            String labelBranchTrue = generator.generateLabel();
            String labelBranchFalse = generator.generateLabel();
            String labelJoinTrue = generator.generateLabel();
            String labelJoinFalse = generator.generateLabel();
            Variable conditionVariable = Variable.nextTemporaryVariable();
            Variable assertionVariable = Variable.nextTemporaryVariable();


            // Tc := E [ If.Condition ]
            Value conditionValue = generator.translateExpression(ifStatement.condition);
            generator.emit(ArithmeticAssignment.newInstruction(
                    conditionVariable, new Constant(0), AssignStatement.ModificationOperator.XOR,
                    conditionValue));
            generator.emitPendingDestruction();

            // Tc != False -> Then(Tc)Else
            generator.emitWithParameters(new ConditionalExit(labelBranchTrue, labelBranchFalse,
                            generator.promoteForConditional(conditionVariable)),
                    conditionVariable);

            generateBranch(labelBranchTrue, labelJoinTrue, ifStatement.thenStatements,
                    conditionVariable, ifStatement.condition,
                    assertionVariable, ifStatement.assertion);
            // Then(Tc) <-                    // Else(Tc) <-
            // 0 := Tc ^ E [ Condition ]      // 0 := Tc ^ E [ Condition ]
            // Ta := 0 ^ E [ Assertion ]      // Ta := 0 ^ E [ Assertion ]
            // -> JoinThen(Ta)                // -> JoinElse(Ta)
            generateBranch(labelBranchFalse, labelJoinFalse, ifStatement.elseStatements,
                    conditionVariable, ifStatement.condition,
                    assertionVariable, ifStatement.assertion);

            // JoinThen(Ta)JoinElse <- Ta is True
            generator.emitWithParameters(new ConditionalEntry(labelJoinTrue, labelJoinFalse,
                            generator.promoteForConditional(assertionVariable)),
                    assertionVariable);

            // Destroy passed variable.
            Value assertionValue = generator.translateExpression(ifStatement.assertion);
            generator.emit(ArithmeticAssignment.newInstruction(
                    new Constant(0), assertionVariable,
                    AssignStatement.ModificationOperator.XOR, assertionValue));
            generator.emitPendingDestruction();

        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {

            String labelEntry = generator.generateLabel();
            String labelLoop = generator.generateLabel();
            String labelRepeat = generator.generateLabel();
            String labelExit = generator.generateLabel();
            Variable entryVariable = Variable.nextTemporaryVariable();
            Variable exitVariable = Variable.nextTemporaryVariable();

            // Ta := 0 + E [ Loop.EntryCondition ]
            Value entryCondition = generator.translateExpression(loopStatement.fromCondition);
            generator.emit(ArithmeticAssignment.assignWithoutDestroy(entryVariable,
                    generator.promoteForArithmeticAssignment(entryCondition)));
            generator.emitPendingDestruction();

            // -> Entry(Ta)
            generator.emitWithParameters(new UnconditionalExit(labelEntry), entryVariable);
            {
                // Entry(Ta)Repeat <- Ta is True
                generator.emitWithParameters(new ConditionalEntry(labelEntry, labelRepeat,
                                generator.promoteForConditional(entryVariable)),
                        entryVariable);

                // Destroy condition passed to this block.
                Value passedCondition = generator.translateExpression(loopStatement.fromCondition);
                generator.emit(ArithmeticAssignment.newInstruction(
                        new Constant(0), entryVariable,
                        AssignStatement.ModificationOperator.XOR, passedCondition));
                generator.emitPendingDestruction();

                // E [ Loop.FirstBody ]
                loopStatement.doStatements.forEach(generator::translateStatement);

                // Tx := E [ Loop.ExitCondition ]
                Value exitCondition = generator.translateExpression(loopStatement.untilCondition);
                generator.emit(ArithmeticAssignment.newInstruction(
                        exitVariable, new Constant(0), AssignStatement.ModificationOperator.XOR,
                        exitCondition));
                generator.emitPendingDestruction();

                // Tx != 0 -> Exit(Tx)Loop
                generator.emitWithParameters(new ConditionalExit(labelExit, labelLoop,
                                generator.promoteForConditional(exitVariable)),
                        exitVariable);
            }
            {
                // Loop(Tx) <-
                generator.emit(new UnconditionalEntry(labelLoop));

                // 0 := Tx ^ E [ Loop.ExitCondition ]
                Value exitCondition = generator.translateExpression(loopStatement.untilCondition);
                generator.emit(ArithmeticAssignment.newInstruction(
                        new Constant(0), exitVariable, AssignStatement.ModificationOperator.XOR,
                        exitCondition));
                generator.emitPendingDestruction();

                // E [ Loop.SecondBody ]
                loopStatement.loopStatements.forEach(generator::translateStatement);

                // Ta := 0 + E [ Loop.EntryCondition ]
                Value repeatedCondition = generator.translateExpression(loopStatement.fromCondition);
                generator.emit(ArithmeticAssignment.assignWithoutDestroy(entryVariable,
                        generator.promoteForArithmeticAssignment(repeatedCondition)));
                generator.emitPendingDestruction();

                // -> Loop(Ta)
                generator.emitWithParameters(new UnconditionalExit(labelRepeat), entryVariable);
            }
            // Exit(Tx) <-
            generator.emit(new UnconditionalEntry(labelExit));

            // 0 := Tx ^ E [ Loop.ExitCondition ]
            Value exitCondition = generator.translateExpression(loopStatement.untilCondition);
            generator.emit(ArithmeticAssignment.newInstruction(
                    new Constant(0), exitVariable, AssignStatement.ModificationOperator.XOR,
                    exitCondition));
            generator.emitPendingDestruction();
        }
    }


    /**
     * This concrete subclass of {@link StatementRSSAGenerator} implements control flow operation using reversed
     * evaluation and copying of the {@link RSSAGenerator#clearDestructionStack()} method.
     * <p>
     * If an {@link Expression} <var>E</var> is used as an condition to select between multiple jumping targets,
     * following code will be generated by this {@link StatementRSSAGenerator}:
     * <ol>
     *  <li><var>E</var> is evaluated into a comparing {@link BinaryOperand} <var>C</var> holding the result.</li>
     *  <li><var>C</var> is used to select the jumping target.</li>
     *  <li>An additional basic block is placed between the condition and both possible targets, in which <var>E</var>
     *  is cleaned up explicitly using the <i>destruction stack</i> gained by previously computing <var>E</var>.</li>
     * </ol>
     * </p>
     * <p>
     * The code generated by this implementation makes efficient use of the capabilities provided by RSSAs conditional
     * entry and exit instructions. The following example is used to demonstrate this using the translation for
     * <code>if (2 + x) > 0 then ... else ... </code> :
     *
     * <pre>
     *  Ta := 0 ^ (2 + x)
     *  Ta > 0 -> LTrue () LFalse
     *
     *  LTrue () <-
     *  0 := Ta ^ (2 + x)
     *  -> LThen ()
     *
     * LFalse () <-
     * 0 := Ta ^ (2 + x)
     * -> LElse()
     * </pre>
     *
     * <p>
     * As it can be seen, this method produces very efficient conditionals at the cost of a more complex implementation
     * logic. Problematic is the generation of <code>fi</code> conditions, where the information of how to clean up
     * an expression in the code generator has to be known before code for the expression itself is generated.
     * To solve this dilemma, this kind of conditions is generated in reverse.
     * <p>
     * <b>Advantages</b> of this implementation include the efficient translation of conditionals, only evaluating the
     * underlying condition once while creating it and once while cleaning up. In cases, where evaluation of the
     * expression does not introduce additional variables, no calculations are needed and the condition can be used
     * as part of the instruction directly.
     * </p>
     */
    public static final class ReversingConditionalGenerator extends StatementRSSAGenerator {

        public ReversingConditionalGenerator(RSSAGenerator generator) {
            super(generator);
        }

        private Deque<Instruction> generateCondition(Expression condition, String trueLabel, String falseLabel) {
            // E [ Condition ] -> True () False
            Value rawCondition = generator.translateExpression(condition);
            generator.emit(new ConditionalExit(
                    trueLabel, falseLabel, generator.promoteForConditional(rawCondition)));
            return generator.clearDestructionStack();
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            // Prepare names used to implement control flow.
            String labelBranchThen = generator.generateLabel();
            String labelBranchElse = generator.generateLabel();
            String labelJoinThen = generator.generateLabel();
            String labelJoinElse = generator.generateLabel();

            // Buffer code generated for the fi-condition to emit it after both branches.
            List<Instruction> joinedBlockBuffer = new ArrayList<>();

            // If-Expression and conditional branch is generated immediately.
            Deque<Instruction> conditionCleanup = generateCondition(ifStatement.condition, labelBranchThen, labelBranchElse);
            // Fi-Expression and conditional join is buffered.
            Deque<Instruction> assertionCleanup = generator.emitBuffered(joinedBlockBuffer, () ->
                    generateCondition(ifStatement.assertion, labelJoinThen, labelJoinElse));

            {
                generator.emit(new UnconditionalEntry(labelBranchThen));
                generator.emitDestructionStack(conditionCleanup);
                ifStatement.thenStatements.forEach(generator::translateStatement);
                generator.emitReversed(() -> generator.emitDestructionStack(assertionCleanup));
                generator.emit(new UnconditionalExit(labelJoinThen));
            }
            {
                generator.emit(new UnconditionalEntry(labelBranchElse));
                generator.emitDestructionStack(conditionCleanup);
                ifStatement.elseStatements.forEach(generator::translateStatement);
                generator.emitReversed(() -> generator.emitDestructionStack(assertionCleanup));
                generator.emit(new UnconditionalExit(labelJoinElse));
            }

            generator.emitReversed(() -> joinedBlockBuffer.forEach(generator::emit));
        }

        private BinaryOperand translateCondition(Expression expression) {
            if (expression instanceof BinaryExpression binary) {
                RValue lhs = generator.promoteToRValue(generator.translateExpression(binary.lhs));
                RValue rhs = generator.promoteToRValue(generator.translateExpression(binary.rhs));
                return new BinaryOperand(lhs, ExpressionRSSAGenerator.translateOperator(binary.operator), rhs);
            } else {
                return (BinaryOperand) generator.translateExpression(expression);
            }
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            String labelEntry = generator.generateLabel();
            String labelLoopBody = generator.generateLabel();
            String labelRepeat = generator.generateLabel();
            String labelExit = generator.generateLabel();
            Deque<Instruction> destroyEntryCondition;
            Deque<Instruction> destroyExitCondition;

            // E [ Loop.EntryCondition ]
            BinaryOperand entryCondition = translateCondition(loopStatement.fromCondition);
            destroyEntryCondition = generator.clearDestructionStack();

            // -> Entry
            generator.emit(new UnconditionalExit(labelEntry));
            {
                // Entry(Ta)Repeat <- Ta is True
                generator.emit(new ConditionalEntry(labelEntry, labelRepeat,
                        entryCondition));
                // Destroy [ Loop.EntryCondition ]
                generator.emitDestructionStack(destroyEntryCondition);

                // E [ Loop.FirstBody ]
                loopStatement.doStatements.forEach(generator::translateStatement);

                // E [ Loop.ExitCondition ] -> Exit()Loop
                generator.emit(new ConditionalExit(labelExit, labelLoopBody, translateCondition(loopStatement.untilCondition)));
                destroyExitCondition = generator.clearDestructionStack();
            }
            {
                // Loop(Tx) <-
                generator.emit(new UnconditionalEntry(labelLoopBody));
                // Destroy [ Loop.ExitCondition ]
                generator.emitDestructionStack(destroyExitCondition);

                // E [ Loop.SecondBody ]
                loopStatement.loopStatements.forEach(generator::translateStatement);

                // E [ Loop.EntryCondition ]
                generator.emitReversed(() ->
                        generator.emitDestructionStack(destroyEntryCondition));
                // -> Loop(Ta)
                generator.emit(new UnconditionalExit(labelRepeat));
            }
            // Exit(Tx) <-
            generator.emit(new UnconditionalEntry(labelExit));
            // Destroy [ Loop.ExitCondition ]
            generator.emitDestructionStack(destroyExitCondition);
        }
    }
}
