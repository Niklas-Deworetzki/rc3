package rc3.januscompiler.tac;

import rc3.januscompiler.Direction;
import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.VisitableTree;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.tac.instances.ArithmeticInstruction;
import rc3.januscompiler.tac.instances.CallInstruction;
import rc3.januscompiler.tac.instances.StoreInstruction;
import rc3.lib.utils.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * This abstract class is used to generate code for {@link Statement} and
 * {@link ProcedureDeclaration} nodes.
 * <p>
 * There are two concrete implementations of this class, used to generate code
 * for forwards and backwards direction.
 * </p>
 * <p>
 * The "symmetric" statements are implemented in this abstract superclass, so the two
 * implementations do not have to implement the same functionality twice.
 * The two statements that are the same forward as backward are:
 * <ul>
 *  <li>{@link SkipStatement}</li>
 *  <li>{@link SwapStatement}</li>
 * </ul>
 * <p>
 * Additionally the {@link CallStatement} is also implemented in this abstract class, since
 * just the called procedure is different in the generated instructions.
 * </p>
 * <p>
 * To reason about the concrete implementation, the method {@link StatementTacGenerator#getDirection()}
 * is used. This allows to select the correct direction for the called procedure in {@link CallStatement}s
 * as well as a guard when processing {@link ProcedureDeclaration}s.
 * Normally the actions performed for a {@link ProcedureDeclaration} are the same forward as backward.
 * <ul>
 *  <li>The generator state is prepared by calling {@link TacGenerator#prepareProcedure(ProcedureInvocation)}.</li>
 *  <li>The {@link Environment} is set up with parameters and local variables.</li>
 *  <li>Procedure prologue and epilogue are generated.</li>
 * </ul>
 * However the {@link MainProcedureDeclaration} is only processed in forward direction, since it is only
 * allowed to exist in a forward state. Calling or un-calling "main" is also forbidden, so the backward
 * execution of "main" can never be reached.
 * </p>
 *
 * @see ForwardTacGeneratingVisitor
 * @see BackwardTacGeneratingVisitor
 */
abstract class StatementTacGenerator extends DoNothingVoidTreeVisitor {
    private final Direction direction;
    protected final TacGenerator generator;

    public StatementTacGenerator(TacGenerator generator, Direction direction) {
        this.generator = generator;
        this.direction = direction;
    }

    @Override
    public void accept(VisitableTree visitable) {
        generator.setActiveTreeNode((Tree) visitable);
        super.accept(visitable);
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * This method is used to generate the bodies of various {@link Statement} nodes.
     *
     * @param statements The body of another {@link Statement}.
     */
    public abstract void generateStatements(List<Statement> statements);

    /**
     * Visiting a {@link ProcedureDeclaration} prepares the {@link TacGenerator} state for the
     * generation of the procedures body.
     * <p>
     * Using {@link StatementTacGenerator#getDirection()} a {@link ProcedureInvocation} for the current
     * procedure is created and used for {@link TacGenerator#prepareProcedure(ProcedureInvocation)}.
     * </p>
     * <p>
     * The {@link Environment} is prepared and the procedure's body is generated.
     * </p>
     * <p>
     * The procedures prologue and epilogue is generated before and after the body.
     * </p>
     *
     * @param procedureDeclaration The {@link ProcedureDeclaration} to generate.
     */
    @Override
    public void visitVoid(ProcedureDeclaration procedureDeclaration) {
        generator.generatePrologue(procedureDeclaration.entry, getDirection());

        // Generate the body with updated environment.
        for (var parameter : procedureDeclaration.parameters) {
            generator.activeTable.parameters.add(generator.getVariable(parameter.entry));
            generator.environment.enter(parameter.entry);
        }
        generateStatements(procedureDeclaration.body);
        generator.environment.clearLocalVariables();

        generator.generateEpilogue();
    }

    /**
     * Prepares the state of {@link TacGenerator} for generation of prologue, body and epilogue of the
     * {@link MainProcedureDeclaration}. The {@link Environment} is updated with all local variables.
     * <p>
     * The prologue of the main procedure initializes all local variables with zero.
     * </p>
     *
     * @param mainProcedure The {@link ProcedureDeclaration} to generate.
     * @implNote This method exits if {@link StatementTacGenerator#getDirection()}
     * returns {@link Direction#BACKWARD} since the main procedure is only executed forwards.
     * @see StatementTacGenerator#visitVoid(ProcedureDeclaration)
     */
    @Override
    public void visitVoid(MainProcedureDeclaration mainProcedure) {
        if (getDirection() == Direction.BACKWARD) {
            return; // Procedure "main" only has forward direction.
        }
        generator.generatePrologue(mainProcedure.entry, Direction.FORWARD);

        // Generate the body with updated environment.
        for (VariableDeclaration localVariable : mainProcedure.variables) {
            generator.activeTable.localDeclarations.add(generator.getVariable(localVariable.entry));
            generator.environment.enter(localVariable.entry);
        }
        generateStatements(mainProcedure.body);
        generator.environment.clearLocalVariables();

        generator.generateEpilogue();
    }

    /**
     * Implement a default-action for {@link BuiltinStatement}.
     * <p>
     * The default action for {@link BuiltinStatement} is to generate a procedure call
     * to the corresponding procedure.
     * </p>
     * <p>
     * The jump target for the {@link CallInstruction} is determined by
     * {@link NameResolver#builtinProcedureName(Builtins.Procedure)}
     * </p>
     *
     * @param statement The {@link BuiltinStatement} to be converted into a procedure call.
     */
    @Override
    public void visitVoid(BuiltinStatement statement) {
        ProcedureEntry entry = generator.environment.getProcedure(statement.builtinProcedure.getName()).orElseThrow();
        String calledProcedure = generator.namer.builtinProcedureName(statement.builtinProcedure);
        // Call builtin like regular procedure.
        generator.generateCall(calledProcedure, statement.argumentList, entry.parameterTypes);
    }

    /**
     * Code for {@link CallStatement} is implemented in this abstract superclass.
     * <p>
     * The code is generated by a call to {@link TacGenerator#generateCall(String, List, List)}.
     * To select the jump target for the {@link CallInstruction}, the
     * method {@link NameResolver#getProcedureName(ProcedureInvocation)} is used.
     * </p>
     * <p>
     * The {@link ProcedureInvocation} required for name resolution contains the {@link ProcedureEntry}
     * for the called procedure, which is fetched from the {@link Environment}.
     * The {@link Direction} used is the result of {@link StatementTacGenerator#getDirection()}.
     * Unless {@link CallStatement#isUncall()} is <code>true</code>. In this case, the {@link Direction}
     * is inverted using {@link Direction#invert()}.
     * </p>
     *
     * @param callStatement The {@link CallStatement} to generate code for.
     */
    @Override
    public void visitVoid(CallStatement callStatement) {
        ProcedureEntry calledProcedure = generator.environment.getProcedure(callStatement.procedureName)
                .orElseThrow(); // ProcedureEntry should always be present.

        // Get direction of called procedure.
        Direction callDirection = getDirection();
        if (callStatement.isUncall()) {
            callDirection = callDirection.invert();
        }

        // Create target label from known information.
        ProcedureInvocation invocation = new ProcedureInvocation(calledProcedure, callDirection);
        String procedureLabel = generator.namer.getProcedureName(invocation);

        // Generate Call instruction to label.
        generator.generateCall(procedureLabel, callStatement.argumentList, calledProcedure.parameterTypes);
    }

    @Override
    public void visitVoid(SkipStatement skipStatement) {
        // Nothing to do here.
    }

    @Override
    public void visitVoid(SwapStatement swapStatement) {
        // Load lhs address and value.
        var lhsVar = swapStatement.lhsvar.accept(generator.operandGenerator);
        var lhsVal = generator.emitLoad(lhsVar);
        // Load rhs address and value.
        var rhsVar = swapStatement.rhsvar.accept(generator.operandGenerator);
        var rhsVal = generator.emitLoad(rhsVar);
        // Store lhs value at rhs address and rhs value at lhs address.
        generator.emit(new StoreInstruction(lhsVar, rhsVal));
        generator.emit(new StoreInstruction(rhsVar, lhsVal));
    }


    /**
     * This class implements the generation of code for the forward direction of {@link Statement Statements}.
     *
     * @see StatementTacGenerator
     * @see BackwardTacGeneratingVisitor
     */
    static class ForwardTacGeneratingVisitor extends StatementTacGenerator {
        public ForwardTacGeneratingVisitor(TacGenerator generator) {
            super(generator, Direction.FORWARD);
        }

        @Override
        public void generateStatements(List<Statement> statements) {
            statements.forEach(this);
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            ArithmeticInstruction.Operator operator = Map.of(
                            AssignStatement.ModificationOperator.ADD, ArithmeticInstruction.Operator.ADD,
                            AssignStatement.ModificationOperator.SUB, ArithmeticInstruction.Operator.SUB,
                            AssignStatement.ModificationOperator.XOR, ArithmeticInstruction.Operator.XOR).
                    get(assignStatement.modificationOperator);

            generator.generateReversibleAssignment(assignStatement.variable, assignStatement.value, operator);
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            switch (builtinStatement.builtinProcedure) {
                case STACK_POP -> generator.generateStackPop(
                        builtinStatement.argumentList.get(0),
                        builtinStatement.argumentList.get(1));
                case STACK_PUSH -> generator.generateStackPush(
                        builtinStatement.argumentList.get(0),
                        builtinStatement.argumentList.get(1));
                default -> super.visitVoid(builtinStatement);
            }
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            generator.generateReversibleBranch(this, ifStatement, ifStatement.condition, ifStatement.assertion);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            generator.generateLocalBlock(this, localBlockStatement.localDeclaration.entry,
                    localBlockStatement.localInitializer, localBlockStatement.delocalInitializer,
                    localBlockStatement.body);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            generator.generateReversibleLoop(this, loopStatement.fromCondition, loopStatement.doStatements,
                    loopStatement.untilCondition, loopStatement.loopStatements);
        }
    }


    /**
     * This class implements the generation of code for the backward direction of {@link Statement Statements}.
     *
     * @see StatementTacGenerator
     * @see ForwardTacGeneratingVisitor
     */
    static class BackwardTacGeneratingVisitor extends StatementTacGenerator {
        public BackwardTacGeneratingVisitor(TacGenerator generator) {
            super(generator, Direction.BACKWARD);
        }

        @Override
        public void generateStatements(List<Statement> statements) {
            CollectionUtils.reverse(statements).forEach(this);
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            ArithmeticInstruction.Operator operator = Map.of(
                            AssignStatement.ModificationOperator.ADD, ArithmeticInstruction.Operator.SUB,
                            AssignStatement.ModificationOperator.SUB, ArithmeticInstruction.Operator.ADD,
                            AssignStatement.ModificationOperator.XOR, ArithmeticInstruction.Operator.XOR).
                    get(assignStatement.modificationOperator);

            generator.generateReversibleAssignment(assignStatement.variable, assignStatement.value, operator);
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            switch (builtinStatement.builtinProcedure) {
                case STACK_POP -> generator.generateStackPush(
                        builtinStatement.argumentList.get(0),
                        builtinStatement.argumentList.get(1));
                case STACK_PUSH -> generator.generateStackPop(
                        builtinStatement.argumentList.get(0),
                        builtinStatement.argumentList.get(1));
                default -> super.visitVoid(builtinStatement);
            }
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            generator.generateReversibleBranch(this, ifStatement, ifStatement.assertion, ifStatement.condition);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            generator.generateLocalBlock(this, localBlockStatement.delocalDeclaration.entry,
                    localBlockStatement.delocalInitializer, localBlockStatement.localInitializer,
                    localBlockStatement.body);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            generator.generateReversibleLoop(this, loopStatement.untilCondition, loopStatement.doStatements,
                    loopStatement.fromCondition, loopStatement.loopStatements);
        }
    }
}