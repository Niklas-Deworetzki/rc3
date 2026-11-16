package rc3.januscompiler.backend.interpreter;

import org.jline.utils.InputStreamReader;
import rc3.januscompiler.Direction;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static rc3.lib.utils.CollectionUtils.reverse;

/**
 * This class implements a simple interpreter for the Janus programming language.
 * <p>
 * To interpret a {@link Program}, a new {@link Interpreter} instance has to be created. The constructor of this
 * class accepts the program to be executed as a parameter.<br/>
 * The execution of the interpreter is started by invoking the {@link Interpreter#run()} method, which will bootstrap
 * the initial stack frame and subsequently will execute the statement of the main procedure.<br/>
 * <b>The interpreted program must be semantically valid. Otherwise runtime exceptions may occur.</b>
 * </p>
 * <p>
 * The interpretation is split into three parts, which are implemented in distinct {@link TreeVisitor} instances.<br/>
 * To execute {@link Statement}s, instances of the abstract {@link Executor} class are used. There are two instances
 * defined as a subclass of {@link Interpreter} which represent forward and backward execution of statements.<br/>
 * {@link Expression} and {@link Variable} nodes are evaluated using the {@link EvaluationVisitor} which is used in both
 * forward and backward execution.
 * </p>
 * <p>
 * If the executed program performs an undefined operation, a {@link FailedAssertion} is thrown. It is populated
 * with the stack trace of the executed program and may be used accordingly.
 * </p>
 *
 * @see ForwardExecutionVisitor
 * @see BackwardExecutionVisitor
 * @see EvaluationVisitor
 */
public final class Interpreter implements Runnable {
    // This fields are used to implement input and output operations.
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final PrintStream out;

    public final Deque<StackFrame> runtimeStack = new ArrayDeque<>();
    public final Environment environment = new Environment();
    public final Program program;

    public final Executor
            forwardExecution = new ForwardExecutionVisitor(),
            backwardExecution = new BackwardExecutionVisitor();
    public final TreeVisitor<Object> evaluationVisitor = new EvaluationVisitor();

    final MainProcedureDeclaration mainProcedureDeclaration;

    public Interpreter(Program program, PrintStream out) {
        this.program = program;
        this.out = out;
        this.mainProcedureDeclaration = (MainProcedureDeclaration) findProcedure(new Identifier("main"));
    }

    @Override
    public void run() throws FailedAssertion {
        try {
            // Prepare stack and environment for execution.
            StackFrame initialStackFrame = new StackFrame(forwardExecution, program, mainProcedureDeclaration);
            for (VariableDeclaration localVariable : mainProcedureDeclaration.variables) {
                environment.enter(localVariable.entry);
                initialStackFrame.enter(localVariable.entry);
            }

            runtimeStack.push(initialStackFrame);
            forwardExecution.execute(mainProcedureDeclaration.body);
        } catch (StackOverflowError stackOverflow) {
            stackOverflow.setStackTrace(StackFrame.buildStackTrace(runtimeStack.element().callerPosition, runtimeStack));
            throw stackOverflow;
        }
    }

    /**
     * Returns the currently active stack frame.
     */
    public StackFrame currentStackFrame() {
        return runtimeStack.peek();
    }

    private ProcedureDeclaration findProcedure(Identifier name) {
        return program.procedures.stream()
                .filter(dec -> dec.name.equals(name))
                .findAny().orElseThrow();
    }

    /**
     * Uses an {@link EvaluationVisitor} instance to evaluate the given {@link Expression} or {@link Variable} and
     * casts the result into the requested type.
     *
     * @param tree The node to evaluate.
     * @param <T>  The type which the evaluation result should be casted to.
     * @return The evaluated result of the given node casted to the requested type.
     * @throws ClassCastException if the evaluation result cannot be casted to the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T evaluate(Tree tree) throws ClassCastException {
        return (T) tree.accept(evaluationVisitor);
    }

    private void validateAssertion(Tree assertingNode, boolean assertion, String fmt, Object... args)
            throws FailedAssertion {
        if (!assertion) {
            FailedAssertion failedAssertion = new FailedAssertion(String.format(fmt, args));
            failedAssertion.setStackTrace(StackFrame.buildStackTrace(assertingNode.position, runtimeStack));
            throw failedAssertion;
        }
    }

    private void throwJanusIO(Statement statement, IOException ioException) throws RuntimeException {
        String message = ioException.getMessage();
        if (message == null || message.isEmpty()) {
            message = ioException.toString();
        }
        RuntimeException exception = new RuntimeException(message);
        exception.setStackTrace(StackFrame.buildStackTrace(statement.position, runtimeStack));
        throw exception;
    }

    private static boolean isTrue(int i) {
        return i != 0;
    }

    private static boolean isFalse(int i) {
        return i == 0;
    }

    private static int encodeBoolean(boolean b) {
        return b ? 1 : 0;
    }

    /**
     * Abstract {@link Executor} class.
     * <p>
     * This class provides methods for the following actions.
     * Since those can be parameterized to behave correctly in both execution directions.
     *     <ul>
     *         <li>Performing a procedure (un-)call.</li>
     *         <li>Executing a local block.</li>
     *         <li>Executing a reversible branch.</li>
     *         <li>Executing a reversible loop.</li>
     *         <li>Push-ing an element to a stack.</li>
     *         <li>Pop-ing an element from a stack.</li>
     *         <li>Reading a value into a store.</li>
     *         <li>Writing a value from a store.</li>
     *     </ul>
     * <p>
     * The execution direction for these methods is specified as an {@link Executor} object passed as a parameter.
     * </p>
     * <p>
     *     Lastly this abstract class also implements the {@link TreeVisitor} interface and provides an implementation
     *     for the following {@link TreeVisitor#visit} methods, since they behave the same in forward and backward execution:
     *     <ul>
     *         <li>{@link TreeVisitor#visit(SkipStatement)}</li>
     *         <li>{@link TreeVisitor#visit(SwapStatement)}</li>
     *     </ul>
     * </p>
     * <p>
     * There exist two implementations of this abstract class, which are specified in the same file.
     *
     * @see ForwardExecutionVisitor
     * @see BackwardExecutionVisitor
     */
    public abstract class Executor extends DoNothingVoidTreeVisitor {
        public final Direction executionDirection;

        public Executor(Direction executionDirection) {
            this.executionDirection = executionDirection;
        }

        public Direction getExecutionDirection() {
            return executionDirection;
        }

        public abstract void execute(List<Statement> statements);

        /**
         * This method implements procedure calls and un-calls.
         * <p>
         * Both calls and un-calls require the abstract syntax of the called procedure to be extracted from
         * the <i>abstract syntax tree</i>, since it contains the list of statements to be executed.
         * </p>
         * <p>
         * The next step in calling and un-calling a procedure is to prepare the runtime for execution of said
         * procedure. To achieve this, the stack frame of the called procedure is initialized at first and pushed
         * onto the runtime stack.
         * Afterwards the arguments passed to the procedure are evaluated one by one and placed in the newly
         * initialized stack frame.
         * Before executing the procedures body, the environment is prepared by placing a reference for all
         * parameters in it.<br/>
         * After the execution is completed, the runtime's previous state has to be restored by popping the stack
         * frame back from the runtime stack and removing all parameters from the environment.
         * </p>
         * <p>
         * The only difference between calling and un-calling a procedure is the execution of the procedure's body.
         * To correctly implement this behavior, an {@link Executor} is passed as a parameter of this method, which
         * is used to handle the execution of the called procedure's body.
         * </p>
         * <p>
         * Additionally the calling statement is passed as an parameter of this method. This information is used
         * to track the execution and provide more relevant information in case of a runtime error.
         * </p>
         *
         * @param caller        The statement initializing the procedure's (un-)call. This parameter is used to track
         *                      execution and provide more relevant information in case of a runtime error.
         * @param executor      The executor handles execution of the procedure's body and is used to determine
         *                      the execution direction.
         * @param procedureName The name of the (un-)called procedure is required to determine, which procedure has to
         *                      be executed.
         * @param argumentList  The arguments in this list are evaluated one by one and passed to
         *                      the (un-)called procedure.
         */
        protected void executeProcedureCall(Statement caller, Executor executor,
                                            Identifier procedureName, List<Variable> argumentList) {
            ProcedureDeclaration calledProcedure = findProcedure(procedureName);
            StackFrame calleeFrame = new StackFrame(executor, caller, calledProcedure);

            Iterator<Variable> arguments = argumentList.iterator();
            Iterator<ParameterType> parameters = calledProcedure.entry.parameterTypes.iterator();

            // Setup stack frame of called procedure.
            while (arguments.hasNext()) {
                Store evaluatedArgument = evaluate(arguments.next());
                calleeFrame.placeArgument(parameters.next(), evaluatedArgument);
            }
            runtimeStack.push(calleeFrame);

            // Prepare environment for call.
            for (VariableDeclaration parameter : calledProcedure.parameters) {
                environment.enter(parameter.entry);
            }

            // Execute procedure's body.
            executor.execute(calledProcedure.body);

            // Restore runtime stack and environment.
            runtimeStack.pop();
            for (VariableDeclaration parameter : reverse(calledProcedure.parameters)) {
                environment.exit(parameter.entry);
            }
        }

        /**
         * This method implements forward and backward execution of local variable blocks, which introduce a new
         * local variable to a block of statements.
         * <p>
         * Both forward and backward execution of these blocks, introduce a new variable and a {@link Store} has
         * to be prepared for it. This {@link Store} lies on the stack within the currently active stack frame.
         * Additionally the introduced variable has to be added to the environment before the body of a local variable
         * block is executed.
         * After execution the variable is removed from the environment again, as well as it is purged from the
         * currently active stack frame.
         * </p>
         * <p>
         * To distinguish between forward and backward execution, an {@link Executor} object is passed as an parameter
         * for this method. This {@link Executor} handles the correct execution of the blocks body.
         * </p>
         * <p>
         * Another difference between both execution directions is how the introduced variable is initialized.
         * The beginning and the end of a local variable block is marked by an {@link Expression} belonging to the
         * introduced variable.
         * During forward execution the upper expression is used to initialize the local variable
         * and the lower expression serves as an assertion.
         * During backward execution however, the upper expression is used as an assertion and the lower expression
         * serves as the local variable's initializer.
         * To provide this functionality the initializer and assertion are passes as parameters to this method in form
         * of an {@link Expression}.
         * </p>
         *
         * @param statement        The statement to be executed. It's body is executed by the given {@link Executor}.
         * @param executor         The {@link Executor} handles the execution of the local variable block's body and
         *                         is used to determine the execution direction.
         * @param entryDeclaration This declaration is used to introduce the new local variable.
         * @param initializer      The initializer {@link Expression} is evaluated at the beginning of the block's
         *                         execution to determine the value, used to initialize the newly introduced variable.
         * @param exitDeclaration  This declaration used to remove the local variable again after execution of
         *                         the block's body.
         * @param assertion        The assertion {@link Expression} is evaluated at the end of the block's execution
         *                         and compared to the actual value of the variable introduced by the local variable block.
         */
        protected void executeLocalBlock(LocalBlockStatement statement, Executor executor,
                                         VariableDeclaration entryDeclaration, Expression initializer,
                                         VariableDeclaration exitDeclaration, Expression assertion) {
            // Prepare stack frame and environment.
            Store localStore = currentStackFrame().enter(entryDeclaration.entry);
            environment.enter(entryDeclaration.entry);

            // Initialize local variable.
            Object initializedValue = evaluate(initializer);
            localStore.setData(initializedValue);

            executor.execute(statement.body);

            Object assertedValue = evaluate(assertion);
            validateAssertion(assertion, Objects.equals(assertedValue, localStore.getData()),
                    "Value of '%s' should be %s at the end of the local block but is %s",
                    entryDeclaration.name, assertedValue, localStore.getData());

            // Restore stack frame and environment.
            environment.exit(exitDeclaration.entry);
            currentStackFrame().exit(exitDeclaration.entry);
        }

        /**
         * This method implements forward and backward execution of reversible if-statements.
         * <p>
         * In both cases of execution, an if-statement evaluates an expression (the condition) and uses it to decide
         * which branch to execute. After the selected branch is executed, a second expression (the assertion) is
         * evaluated. It's value must match the value of the condition.
         * </p>
         * <p>
         * The difference between forward and backward execution is, which expression of the if-statement is used
         * as expression and assertion. <br/>
         * During forward execution the upper expression is used as the condition and the lower as the assertion.
         * During backward execution those expressions are swapped.
         * </p>
         * <p>
         * To allow for correct behavior during both forward and backward execution, the two expressions are passed
         * as a parameter to this method. Additionally an {@link Executor} object is passed as a parameter, which
         * handles the execution of the selected branch.
         * Since the <code>true</code> and <code>false</code> branch are the same during forward and backward execution, they
         * are fetched directly from the passed {@link IfStatement}.
         * </p>
         * <p>
         * The following diagram describes the control flow of an reversible if-statement.
         * </p>
         * <pre>
         *                      +-------------+
         *              +------>+ then branch +-------+
         *      [true]  |       +-------------+       v [true]
         *       +------+------+               +------+------+
         * ----->+  condition  |               |  assertion  +---->
         *       +------+------+               +------+------+
         *      [false] |       +-------------+       ^ [false]
         *              +------>+ else branch +-------+
         *                      +-------------+
         * </pre>
         *
         * @param statement           The statement to be executed. One of its branches is executed
         *                            by the given {@link Executor}.
         * @param executor            The {@link Executor} handles execution of the selected branch of this if-statement
         *                            and determines the direction of execution.
         * @param conditionExpression The {@link Expression} used as the condition. It is evaluated at the beginning
         *                            of this statements execution and used to determine which branch should be executed.
         * @param assertionExpression The {@link Expression} used as the assertion. It is evaluated at the end of this
         *                            statements execution and must have the same truth-value as the condition.
         */
        protected void executeIfStatement(IfStatement statement, Executor executor,
                                          Expression conditionExpression, Expression assertionExpression) {
            boolean condition = Interpreter.isTrue(evaluate(conditionExpression));

            if (condition) {
                executor.execute(statement.thenStatements);
            } else {
                executor.execute(statement.elseStatements);
            }

            boolean assertion = Interpreter.isTrue(evaluate(assertionExpression));
            validateAssertion(assertionExpression, condition == assertion,
                    "Assertion should be %s but is %s.", condition, assertion);
        }

        /**
         * This method implements forward and backward execution of reversible loops.
         * <p>
         * The execution of a reversible loop starts by evaluating an entry condition. This condition in form of an
         * {@link Expression} must evaluate to <code>true</code> at entry of the loop. Following the evaluation of this
         * expression, the first body is executed. After this execution, a second condition (the exit condition) is
         * evaluated. If it evaluates to <code>false</code> the loop is exited. If it evaluates to <code>true</code>
         * the second body is executed and the execution steps are repeated. In every following steps whenever the
         * entry condition is evaluated again, it must evaluate to <code>false</code>.
         * </p>
         * <p>
         * Since during forward and backward execution, not only the conditions but also the bodies of the loop are
         * swapped, they are all passed as an parameter for this method. Additionally an {@link Executor} object is
         * passed, which handles execution of the bodies.
         * </p>
         * <p>
         * The following diagram describes the control flow of a reversible loop.
         * </p>
         * <pre>
         *                       +-------------+
         *               +------>+ first  body +-------+
         *               |       +-------------+       v
         *        +------+------+               +------+------+
         * [true] |    entry    |               |     exit    | [true]
         * ------>+  condition  |               |  condition  +------->
         *        +------+------+               +------+------+
         *       [false] ^       +-------------+       | [false]
         *               +-------+ second body +<------+
         *                       +-------------+
         * </pre>
         *
         * @param statement       The statement to be executed. It provides the bodies which are executed by the
         *                        {@link Executor}.
         * @param executor        The executor handles execution of the loop's bodies and determines the direction of
         *                        execution.
         * @param entryExpression This {@link Expression} must evaluate to <code>true</code> at the beginning of the loop
         *                        and must always evaluate to <code>false</code> afterwards until the loop is exited.
         * @param untilExpression This {@link Expression} determines how many iterations of the loop have to be executed
         *                        as the loop is exited if this condition evaluates to <code>true</code>.
         */
        protected void executeLoopStatement(LoopStatement statement, Executor executor,
                                            Expression entryExpression, Expression untilExpression) {

            int entryCondition = evaluate(entryExpression);
            validateAssertion(entryExpression, Interpreter.isTrue(entryCondition),
                    "The entry condition must evaluate to true upon entering the loop.");

            executor.execute(statement.doStatements);

            // Loop until the exit condition is met.
            while (Interpreter.isFalse(evaluate(untilExpression))) {
                executor.execute(statement.loopStatements);

                entryCondition = evaluate(entryExpression);
                validateAssertion(entryExpression, Interpreter.isFalse(entryCondition),
                        "The entry condition must evaluate to false upon repeated evaluation.");

                executor.execute(statement.doStatements);
            }
        }

        /**
         * This method implements the push-ing of a value onto a stack from a given variable.
         * <p>
         * As a stacks push and pop operations are inverse to each other, they are implemented outside of the concrete
         * {@link Executor} implementations. This allows reuse of the implementation.
         * </p>
         * <p>
         * Pushing a value onto a stack, requires the evaluation of both, the variables and the stacks {@link Store}.
         * The value of the variable is pushed onto the stack and the variables is zero-cleared afterwards.
         * </p>
         *
         * @param stack    The {@link Variable} pointing to the stack.
         * @param variable The {@link Variable} pointing to the value pushed onto the stack.
         * @see Executor#popStack(Statement, Variable, Variable)
         */
        protected void pushStack(Variable stack, Variable variable) {
            Store.StackStore stackStore = evaluate(stack);
            Store.IntStore variableStore = evaluate(variable);

            stackStore.getData().push(variableStore.getData());
            variableStore.setData(0);
        }

        /**
         * This method implements the pop-ing of a value from a stack into a given variable.
         * <p>
         * As a stacks push and pop operations are inverse to each other, they are implemented outside of the concrete
         * {@link Executor} implementations. This allows reuse of the implementation.
         * </p>
         * <p>
         * Popping a value from a stack, requires the evaluation of both, the variables and the stacks {@link Store}.
         * The top value of the stack is removed from the stack and placed into the variable.
         * </p>
         * <p>
         * This operation fails, if the stack is empty or the variable is not zero-cleared.
         * </p>
         *
         * @param statement The statement to be executed.
         * @param stack     The {@link Variable} pointing to the stack.
         * @param variable  The {@link Variable} pointing to the value pushed onto the stack.
         * @see #pushStack(Variable, Variable)
         */
        protected void popStack(Statement statement, Variable stack, Variable variable) {
            Store.StackStore stackStore = evaluate(stack);
            Store.IntStore variableStore = evaluate(variable);

            validateAssertion(statement, variableStore.getData() == 0,
                    "Can't pop into non-zero variable '%s'.", variable.name);

            validateAssertion(statement, !stackStore.getData().isEmpty(),
                    "Can't pop from empty stack '%s'.", stack.name);

            variableStore.setData(stackStore.getData().pop());
        }

        /**
         * Reads an integer value from the input and stores the result in the given {@link Variable}.
         *
         * @param variable The {@link Variable} to store the input read.
         */
        protected void read(Variable variable) throws IOException {
            write(variable);
            Store.IntStore store = evaluate(variable);
            int read = Integer.parseInt(in.readLine());
            store.setData(read);
        }

        /**
         * Writes the contents of a {@link Variable} to the output.
         *
         * @param variable The {@link Variable} to write.
         */
        protected void write(Variable variable) {
            Store toWrite = evaluate(variable);
            out.println(toWrite);
        }

        /**
         * Reads a single character from the input and stores the result in the given {@link Variable}.
         *
         * @param variable The {@link Variable} to store the input read.
         */
        protected void readc(Variable variable) throws IOException {
            writec(variable);
            Store.IntStore store = evaluate(variable);
            int character = in.read();
            store.setData(character);
        }

        /**
         * Writes the contents of a {@link Variable} as an character to the output.
         *
         * @param variable The {@link Variable} to write.
         */
        protected void writec(Variable variable) {
            Store.IntStore store = evaluate(variable);
            int character = store.getData();
            out.print((char) character);
        }

        ///////////////////////////////////////////////////////////
        //               Symmetric visit methods.                //
        ///////////////////////////////////////////////////////////

        @Override
        public void visitVoid(SkipStatement skipStatement) {
        }

        @Override
        public void visitVoid(SwapStatement swapStatement) {
            Store lhs = evaluate(swapStatement.lhsvar);
            Store rhs = evaluate(swapStatement.rhsvar);

            Object tmp = lhs.getData();
            lhs.setData(rhs.getData());
            rhs.setData(tmp);
        }
    }


    /**
     * The {@link Executor} implementation, that executes statements in forward direction.
     */
    private class ForwardExecutionVisitor extends Executor {

        public ForwardExecutionVisitor() {
            super(Direction.FORWARD);
        }

        @Override
        public void execute(List<Statement> statements) {
            statements.forEach(this);
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            Store.IntStore store = evaluate(assignStatement.variable);
            Integer value = evaluate(assignStatement.value);

            switch (assignStatement.modificationOperator) {
                case ADD -> store.data += value;
                case SUB -> store.data -= value;
                case XOR -> store.data ^= value;
            }
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            List<Variable> arguments = builtinStatement.argumentList;
            try {
                switch (builtinStatement.builtinProcedure) {
                    case READ -> read(arguments.get(0));
                    case WRITE -> write(arguments.get(0));
                    case READ_C -> readc(arguments.get(0));
                    case WRITE_C -> writec(arguments.get(0));
                    case STACK_POP -> popStack(builtinStatement, arguments.get(1), arguments.get(0));
                    case STACK_PUSH -> pushStack(arguments.get(1), arguments.get(0));
                    default -> throw Builtins.builtinNotImplemented(builtinStatement.builtinProcedure);
                }
            } catch (IOException ioException) {
                throwJanusIO(builtinStatement, ioException);
            }
        }

        @Override
        public void visitVoid(CallStatement callStatement) {
            // Keep reversed execution direction, if it is a call and not an uncall.
            Executor executor = callStatement.isCall() ? forwardExecution : backwardExecution;
            super.executeProcedureCall(callStatement, executor,
                    callStatement.procedureName, callStatement.argumentList);

        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            super.executeIfStatement(ifStatement, forwardExecution,
                    ifStatement.condition, ifStatement.assertion);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            super.executeLocalBlock(localBlockStatement, forwardExecution,
                    localBlockStatement.localDeclaration, localBlockStatement.localInitializer,
                    localBlockStatement.delocalDeclaration, localBlockStatement.delocalInitializer);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            super.executeLoopStatement(loopStatement, forwardExecution,
                    loopStatement.fromCondition, loopStatement.untilCondition);
        }
    }


    /**
     * An {@link Executor} implementation that executes statements in backward direction.
     */
    private class BackwardExecutionVisitor extends Executor {

        public BackwardExecutionVisitor() {
            super(Direction.BACKWARD);
        }

        @Override
        public void execute(List<Statement> statements) {
            reverse(statements).forEach(this);
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            Store.IntStore store = evaluate(assignStatement.variable);
            Integer value = evaluate(assignStatement.value);

            switch (assignStatement.modificationOperator) {
                case ADD -> store.data -= value;
                case SUB -> store.data += value;
                case XOR -> store.data ^= value;
            }
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            List<Variable> arguments = builtinStatement.argumentList;
            try {
                switch (builtinStatement.builtinProcedure) {
                    case READ -> read(arguments.get(0));
                    case WRITE -> write(arguments.get(0));
                    case READ_C -> readc(arguments.get(0));
                    case WRITE_C -> writec(arguments.get(0));
                    case STACK_POP -> pushStack(arguments.get(1), arguments.get(0));
                    case STACK_PUSH -> popStack(builtinStatement, arguments.get(1), arguments.get(0));
                    default -> throw Builtins.builtinNotImplemented(builtinStatement.builtinProcedure);
                }
            } catch (IOException ioException) {
                throwJanusIO(builtinStatement, ioException);
            }
        }

        @Override
        public void visitVoid(CallStatement callStatement) {
            // Keep reversed execution direction, if it is a call and not an uncall.
            Executor executor = callStatement.isCall() ? backwardExecution : forwardExecution;
            super.executeProcedureCall(callStatement, executor,
                    callStatement.procedureName, callStatement.argumentList);
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            super.executeIfStatement(ifStatement, backwardExecution,
                    ifStatement.assertion, ifStatement.condition);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            super.executeLocalBlock(localBlockStatement, backwardExecution,
                    localBlockStatement.delocalDeclaration, localBlockStatement.delocalInitializer,
                    localBlockStatement.localDeclaration, localBlockStatement.localInitializer);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            super.executeLoopStatement(loopStatement, backwardExecution,
                    loopStatement.untilCondition, loopStatement.fromCondition);
        }
    }

    private class EvaluationVisitor extends DoNothingTreeVisitor<Object> {

        @Override
        public Integer visit(BinaryExpression binaryExpression) {
            int lhs = evaluate(binaryExpression.lhs);
            int rhs = evaluate(binaryExpression.rhs);

            switch (binaryExpression.operator) {
                case ADD:
                    return lhs + rhs;
                case SUB:
                    return lhs - rhs;
                case MUL:
                    return lhs * rhs;
                case DIV:
                    validateAssertion(binaryExpression, rhs != 0, "Division by 0!");
                    return lhs / rhs;
                case MOD:
                    validateAssertion(binaryExpression, rhs != 0, "Division by 0!");
                    return lhs % rhs;
                case AND:
                    return lhs & rhs;
                case OR:
                    return lhs | rhs;
                case XOR:
                    return lhs ^ rhs;
                case EQU:
                    return encodeBoolean(lhs == rhs);
                case NEQ:
                    return encodeBoolean(lhs != rhs);
                case LST:
                    return encodeBoolean(lhs < rhs);
                case LSE:
                    return encodeBoolean(lhs <= rhs);
                case GRT:
                    return encodeBoolean(lhs > rhs);
                case GRE:
                    return encodeBoolean(lhs >= rhs);
                case CONJ:
                    return encodeBoolean(Interpreter.isTrue(lhs) && Interpreter.isTrue(rhs));
                case DISJ:
                    return encodeBoolean(Interpreter.isTrue(lhs) || Interpreter.isTrue(rhs));

                default:
                    throw new RuntimeException("Unknown binary operator: " + binaryExpression.operator);
            }
        }

        @Override
        public Object visit(BuiltinExpression builtinExpression) {
            Store.StackStore stack = evaluate(builtinExpression.argumentList.get(0));
            switch (builtinExpression.expression) {
                case EMPTY:
                    return Interpreter.encodeBoolean(stack.getData().isEmpty());

                case TOP:
                    validateAssertion(builtinExpression, !stack.getData().isEmpty(),
                            "Can't get top element from empty stack.");
                    return stack.getData().peek();

                default:
                    throw Builtins.builtinNotImplemented(builtinExpression.expression);
            }
        }

        @Override
        public Store visit(IndexedVariable indexedVariable) {
            VariableEntry variable = environment.getVariable(indexedVariable.name).orElseThrow();
            Store.ArrayStore store = (Store.ArrayStore) currentStackFrame().get(variable);
            int index = evaluate(indexedVariable.index);

            validateAssertion(indexedVariable.index, index >= 0 && index < store.getData().length,
                    "Array index (%d) is out of bounds!", index);

            return store.getData()[index];
        }

        @Override
        public Integer visit(IntLiteral intLiteral) {
            return intLiteral.value;
        }

        @Override
        public Store visit(NamedVariable namedVariable) {
            VariableEntry variable = environment.getVariable(namedVariable.name).orElseThrow();
            return currentStackFrame().get(variable);
        }

        @Override
        public Deque<Integer> visit(StackNilLiteral stackNilLiteral) {
            return new Stack<>();
        }

        @Override
        public Object visit(VariableExpression variableExpression) {
            Store store = evaluate(variableExpression.variable);
            return store.getData();
        }
    }
}
