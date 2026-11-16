package rc3.januscompiler.tac;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.januscompiler.backend.AnnotationMode;
import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.backend.RuntimeAssertion;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.table.*;
import rc3.januscompiler.tac.instances.*;
import rc3.januscompiler.tac.table.Procedure;
import rc3.januscompiler.tac.table.Scope;
import rc3.januscompiler.types.PrimitiveType;
import rc3.lib.parsing.Position;

import java.util.*;

/**
 * This class implements a generator for Three-Address-Code {@link Instruction Instructions}.
 * <p>
 * The static method {@link TacGenerator#generateCode(Environment, RuntimeLibrary, Program, AnnotationMode)}
 * provided by this class may be used to generate a {@link List} of {@link Instruction Instructions} for a complete
 * {@link Program}. The {@link Program} is required to be correct (i.e. it has to pass all analysis passes defined
 * in the {@link Compiler} instance).
 * </p>
 * <p>
 * This generator implementation has the ability to generate annotations for the {@link Instruction Instructions}.
 * These annotations are {@link String} values associated with an {@link Instruction} via the
 * {@link Instruction#annotate(String)} method. If {@link AnnotationMode#SOURCE_ANNOTATIONS} is passed to the
 * code generation method, an annotation referencing the original Janus code is generated for every progressed
 * source code line.
 * </p>
 * <p>
 * The generation is split into three different parts. The {@link TacGenerator} class manages the generation, stores
 * the generated {@link Instruction Instructions} and provides predefined implementations for recurring tasks.
 * The generation of {@link Statement Statements} is handled by the concrete {@link StatementTacGenerator} instances.
 * There are two concrete implementations defined for the abstract {@link StatementTacGenerator} class:
 * <ul>
 *  <li>{@link StatementTacGenerator.ForwardTacGeneratingVisitor}</li>
 *  <li>{@link StatementTacGenerator.BackwardTacGeneratingVisitor}</li>
 * </ul>
 * Instances of the concrete implementations handle the forward and backward code generation for {@link Statement} nodes.
 * {@link Expression} and {@link Variable} nodes are translated by {@link ExpressionTacGenerator} instances.
 * The methods {@link TacGenerator#generateVariable(Variable)}, {@link TacGenerator#generateExpression(Expression)} and
 * {@link TacGenerator#emitBranch(Operand, Operand, IfGotoInstruction.RelativeOperator, String)} can be used to interface with the
 * {@link ExpressionTacGenerator} instances.
 * </p>
 * <p>
 * Any {@link TacGenerator} instance is parameterized by a {@link RuntimeLibrary}. The {@link RuntimeLibrary} can be
 * used to tailor the generated {@link Instruction Instructions} to a specific backend.
 * The {@link RuntimeLibrary} has to provide a {@link NameResolver} instance via the
 * {@link RuntimeLibrary#getNameResolver()} method. This {@link NameResolver} handles the mapping of Janus names to
 * valid names for the given backend. It is required to convert the original names since the target backend may not
 * allow the whole range of possible Janus names. When a different scope (or even no scope) is present in the backend
 * it may also be required to change the names to match this restrictions.
 * </p>
 * <p>
 * The stack layout is also determined by the {@link RuntimeLibrary}. Different stack operations are simply
 * delegated to the {@link StackImplementation} the {@link RuntimeLibrary} instance provided. The backend defines:
 * <ul>
 *  <li>The empty stack <code>nil</code>. The required {@link Instruction Instructions} are generated via
 *  {@link StackImplementation#generateNil(TacGenerator)}.</li>
 *  <li>The retrieval of the top element. The required {@link Instruction Instructions} are generated via
 *  {@link StackImplementation#generateTop(TacGenerator, TemporaryOperand)}</li>
 *  <li>The stack push and pop operations. The required {@link Instruction Instructions} are generated via
 *  {@link StackImplementation#generatePush(TacGenerator, TemporaryOperand, TemporaryOperand)} and
 *  {@link StackImplementation#generatePop(TacGenerator, TemporaryOperand, TemporaryOperand)}</li>
 *  <li>The equality constraints for two stacks. The required {@link Instruction Instructions} are generated via
 *  {@link StackImplementation#generateComparison(TacGenerator, Operand, Operand, String, String)}</li>
 *  <li>The assignment semantics for a stack and a variable. The required {@link Instruction Instructions} are
 *  generated via {@link StackImplementation#generateAssignment(TacGenerator, TemporaryOperand, Operand)}</li>
 *  <li>The destruction operation of a stack. The required {@link Instruction Instructions} are generated via
 *  {@link StackImplementation#generateExit(TacGenerator, TemporaryOperand)}</li>
 * </ul>
 * </p>
 * <p>
 * To ensure correct behavior of the generated code, {@link RuntimeAssertion} checks are embedded in the generated
 * code. Most of the assertion {@link Instruction Instructions} are generated automatically in the required manner.
 * The exception to this are the stack assertions:
 * <ul>
 *  <li>{@link RuntimeAssertion#POP_EMPTY_STACK}</li>
 *  <li>{@link RuntimeAssertion#TOP_EMPTY_STACK}</li>
 * </ul>
 * Checks for those have to be generated by the {@link RuntimeLibrary} since there might be more efficient
 * measures to check a stack for <code>nil</code> than the general comparison generated by
 * {@link StackImplementation#generateIsEmpty(TacGenerator, Operand, String, String)}.</br>
 * The other assertion that can be disabled is the explicit check for {@link RuntimeAssertion#DIVISION_BY_ZERO}.
 * If {@link RuntimeLibrary#checkArithmetic()} does not return <code>true</code>, it is assumed that the runtime will
 * handle division by zero implicitly and no {@link Instruction Instructions} for explicit checks are generated.
 * </p>
 *
 * @implNote This class still makes some assumptions about the memory layout of the backend. Arrays are assumed to
 * be consecutive integers in the memory with the index growing towards higher memory locations.
 * @see StatementTacGenerator
 * @see ExpressionTacGenerator
 */
public class TacGenerator {
    // Fields used to read required information for code generation.
    protected final Environment environment;
    protected final RuntimeLibrary runtime;
    protected final StackImplementation stackImplementation;
    protected final NameResolver namer;

    // A generator instance used to generate expressions that are not part of conditions.
    protected final ExpressionTacGenerator operandGenerator = new ExpressionTacGenerator(this);

    // The list where generated instructions are collected.
    protected final List<Instruction> generatedInstructions = new ArrayList<>();

    // Counters used to generate unique names and labels.
    protected int nextTemporaryOperand = 0;
    protected int nextLabel = 0;

    // Scope and Procedure information filled by visiting declarations.
    protected Procedure activeTable;
    protected List<Scope> parentScopeList;

    // Variables to keep track of annotations.
    protected final AnnotationMode annotationMode;
    protected boolean annotationEmitted;
    protected Tree activeTreeNode;

    private TacGenerator(Environment globalEnvironment, RuntimeLibrary runtimeLibrary, AnnotationMode annotationMode) {
        this.environment = globalEnvironment;
        this.runtime = runtimeLibrary;
        this.stackImplementation = runtimeLibrary.getStackImplementation();
        this.namer = runtimeLibrary.getNameResolver();
        this.annotationMode = annotationMode;
    }


    /**
     * Generates the code for all procedures in a program and returns the {@link List} of generated
     * {@link Instruction Instructions}.
     *
     * @param globalEnvironment The global {@link Environment} containing all procedures.
     * @param runtimeLibrary    The {@link RuntimeLibrary} used to generate code.
     * @param program           The {@link Program} containing the procedures to be generated.
     * @param annotationMode    Describes how generated {@link Instruction Instructions} should be annotated.
     * @return A list containing all generated instructions for the program.
     */
    public static List<Instruction> generateCode(Environment globalEnvironment, RuntimeLibrary runtimeLibrary,
                                                 Program program, AnnotationMode annotationMode) {
        var generator = new TacGenerator(globalEnvironment, runtimeLibrary, annotationMode);
        generator.generateProcedureCode(program);
        return generator.generatedInstructions;
    }

    private void generateProcedureCode(Program program) {
        var forwardGenerator = new StatementTacGenerator.ForwardTacGeneratingVisitor(this);
        var backwardGenerator = new StatementTacGenerator.BackwardTacGeneratingVisitor(this);
        // Generate forward and backward code.
        program.procedures.forEach(forwardGenerator.andThen(backwardGenerator));
    }

    /**
     * Returns the {@link AnnotationMode} selected for this {@link TacGenerator} instance.
     *
     * @return The selected {@link AnnotationMode}.
     */
    public AnnotationMode getAnnotationMode() {
        return annotationMode;
    }

    /**
     * Returns the {@link Environment} used by this {@link TacGenerator} instance.
     *
     * @return The used {@link Environment}.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns the {@link RuntimeLibrary} used to specialize the generated {@link Instruction Instructions}.
     *
     * @return The used {@link RuntimeLibrary}.
     */
    public RuntimeLibrary getRuntimeLibrary() {
        return runtime;
    }

    /**
     * Returns the {@link NameResolver} used by this {@link TacGenerator} instance.
     *
     * @return The used {@link NameResolver}.
     * @see RuntimeLibrary#getNameResolver()
     */
    public NameResolver getNameResolver() {
        return namer;
    }

    /**
     * Returns the {@link StackImplementation} used by this {@link TacGenerator} instance.
     *
     * @return The used {@link StackImplementation}.
     * @see RuntimeLibrary#getStackImplementation()
     */
    public StackImplementation getStackImplementation() {
        return stackImplementation;
    }

    /**
     * Emit the given {@link Instruction} appending it to the generated instructions.
     *
     * @param instruction The generated {@link Instruction} to append to the output.
     */
    public void emit(Instruction instruction) {
        if (annotationMode != AnnotationMode.NO_ANNOTATIONS && !annotationEmitted) {
            annotationEmitted = true;
            if (activeTreeNode != null) {
                instruction.annotate(buildSourceAnnotation(activeTreeNode.position));
            }
        }
        generatedInstructions.add(instruction);
    }


    /**
     * Create the {@link String} used to annotate {@link Instruction Instructions}.
     */
    private String buildSourceAnnotation(Position position) {
        Optional<String> sourceLine;

        if (annotationMode == AnnotationMode.SOURCE_ANNOTATIONS &&
                (sourceLine = position.getSourceLine()).isPresent()) {
            return String.format("%d: %s", position.line, sourceLine.get());
        } else { // AnnotationMode.LINE_ANNOTATIONS
            return String.valueOf(position.line);
        }
    }

    /**
     * Updates the {@link Tree} that is currently translated into {@link Instruction Instructions}. The
     * {@link Tree#position} of the active node is used when emitting source code annotations.
     *
     * @param node The {@link Tree} currently translated.
     */
    public void setActiveTreeNode(Tree node) {
        if (node.position == null) return; // Discard positionless nodes.
        if (activeTreeNode == null || activeTreeNode.position.line != node.position.line) {
            this.activeTreeNode = node;
            this.annotationEmitted = false;
        }
    }


    /**
     * Prepares the {@link TacGenerator} state for the generation of a procedure.
     * This resets the counter for fresh temporary variable and label names.
     *
     * @param invocation The procedure that is generated next.
     */
    protected String prepareProcedure(ProcedureInvocation invocation) {
        activeTable = new Procedure(invocation.procedure());
        parentScopeList = activeTable.body;

        nextTemporaryOperand = 0;
        nextLabel = 0;
        namer.initializeScope(invocation);
        activeTreeNode = null;

        return namer.getProcedureName(invocation);
    }

    /**
     * Creates a new unique label for the current procedure.
     *
     * @return A new unique label name.
     */
    public String newLabel() {
        int labelNumber = nextLabel++;
        return namer.getLabelName(labelNumber);
    }

    /**
     * Creates a new unique temporary operand for the current procedure.
     *
     * @return A new unique temporary operand.
     */
    public TemporaryOperand newTempOperand() {
        int operandNumber = nextTemporaryOperand++;
        String operandName = namer.getTemporaryName(operandNumber);
        return new TemporaryOperand(operandName);
    }

    /**
     * Returns an operand containing a reference to an empty stack.
     *
     * @return Operand holding a reference to an empty stack.
     */
    public Operand stackNil() {
        return stackImplementation.generateNil(this);
    }


    /**
     * Creates a new {@link rc3.januscompiler.tac.table.Variable Variable} object for the {@link VariableEntry}.
     * This object extends the information of {@link VariableEntry} by the name that is given to the variable by
     * the {@link NameResolver}.
     *
     * @param entry The {@link VariableEntry} that is stored in the returned
     *              {@link rc3.januscompiler.tac.table.Variable Variable}.
     * @return An object holding the {@link VariableEntry} and the name of an variable.
     */
    public rc3.januscompiler.tac.table.Variable getVariable(VariableEntry entry) {
        String name = namer.getVariableNameInScope(entry);
        return new rc3.januscompiler.tac.table.Variable(name, entry);
    }

    /**
     * Tries to lookup a variable by its {@link Identifier} from the current {@link Environment}. If no variable
     * with the given name is present, this method will throw a {@link NoSuchElementException}.
     * <p>
     * The {@link Identifier} holds the name of the variable in the original Janus source code.
     * </p>
     *
     * @param name The name of the {@link VariableEntry} to lookup.
     * @return The {@link VariableEntry} present in the current {@link Environment} with a matching name.
     * @throws NoSuchElementException If no {@link VariableEntry} can be found.
     */
    public VariableEntry lookupVariable(Identifier name) throws NoSuchElementException {
        return environment.getVariable(name)
                .orElseThrow(() -> new NoSuchElementException("No variable named " + name));
    }

    /**
     * Generates a label for a {@link RuntimeAssertion}. This label should be the target if an assertion fails.
     *
     * @param assertion The {@link RuntimeAssertion} to generate a label for.
     * @return The label for the given {@link RuntimeAssertion}.
     */
    public String failedAssertionLabel(RuntimeAssertion assertion) {
        return namer.failedAssertionLabel(assertion);
    }


    /**
     * Generates a procedure prologue. This includes preparing the generators state,
     * emitting a {@link Label} for the procedure and emitting the {@link BeginInstruction}.
     */
    protected void generatePrologue(ProcedureEntry entry, Direction direction) {
        ProcedureInvocation activeProcedure = new ProcedureInvocation(entry, direction);
        String labelName = prepareProcedure(activeProcedure);

        emit(new Label(labelName));
        emit(new BeginInstruction(labelName, activeTable));
    }

    /**
     * Generates a procedure epilogue. This includes emitting an {@link EndInstruction}
     * and a {@link ReturnInstruction}.
     */
    protected void generateEpilogue() {
        emit(new EndInstruction());
        emit(new ReturnInstruction());
    }


    /**
     * Generates the code for a reversible assignment.
     * <p>
     * Assignments of the form <code>var op= exp</code> are converted to code that is equivalent
     * to <code>var = var op exp</code> in commonly used languages.
     * </p>
     * <p>
     * The generated instructions perform following operations:
     * <ol>
     *  <li>Fetch the variable's address.</li>
     *  <li>Fetch the expression's value.</li>
     *  <li>Load the variable's value.</li>
     *  <li>Apply the operator onto the variable's and the expression's value.</li>
     *  <li>Store the result at the memory location pointed at by the variable's address.</li>
     * </ol>
     * </p>
     *
     * @param variable The variable that is the target of the assignment.
     * @param value    The value used to modify the variable.
     * @param operator The operator used to calculate the new value.
     */
    public void generateReversibleAssignment(Variable variable, Expression value, ArithmeticInstruction.Operator operator) {
        // Get assigned-to address.
        var varAddress = generateVariable(variable);

        // Evaluate modifying expressions.
        var lhs = emitLoad(varAddress);
        var rhs = generateExpression(value);

        // Calculate and store result.
        var result = newTempOperand();
        emit(new ArithmeticInstruction(result, lhs, rhs, operator));
        emit(new StoreInstruction(varAddress, result));
    }

    /**
     * Generates code for a reversible branch statement.
     * <p>
     * At first the condition is evaluated, jumping to the beginning of the
     * {@link IfStatement#thenStatements} branch on <code>true</code>. If the condition
     * evaluates to <code>false</code>, a jump to {@link IfStatement#elseStatements} branch
     * is made.
     * </p>
     * <p>
     * After the code of the condition, the two branches are generated using the given
     * {@link StatementTacGenerator}. A jump introduced by the assertion ensures, that
     * only one body is ever executed.
     * Following the two branches a label is generated, joining the two paths.
     * </p>
     * <p>
     * At the end of every branch the assertion is generated. In the <code>true</code> branch
     * the assertion jumps to the end of the branch statement upon evaluating to <code>true</code>.
     * In the <code>false</code> branch, the assertion jumps to the end of the branch statement
     * upon evaluating to <code>false</code>.
     * </p>
     * <p>
     * If the assertions fail and do not jump to the joining label, a jump to the corresponding error
     * is made.
     * </p>
     * <p>
     * The control flow is depicted by the following diagram:
     * <pre>
     *    [true] +-----------+ [false]
     *      +----+ Condition +----+
     *      |    +-----------+    |
     *      v                     v
     *   +--+---+             +---+--+
     *   | then |             | else |
     *   +--+---+             +---+--+
     *      |                     |
     *      v                     v
     * +----+-----+         +-----+----+
     * | assertion|         |¬assertion|
     * +----+-----+         +-----+----+
     *      | [true]       [true] |
     *      +--------> + <--------+
     *                 |
     *                 v
     * </pre>
     * </p>
     *
     * @param generator The generator used to generate the branches code.
     * @param branch    The {@link IfStatement} holding the two branches.
     * @param condition The {@link Expression} evaluated to decide which branch to choose.
     * @param assertion The {@link Expression} evaluated after executing the chosen branch.
     */
    public void generateReversibleBranch(StatementTacGenerator generator, IfStatement branch,
                                         Expression condition, Expression assertion) {
        String thenBranch = newLabel();
        String elseBranch = newLabel();
        String endOfIf = newLabel();
        String failedAssertionLabel = failedAssertionLabel(RuntimeAssertion.IF_CONDITION_MISMATCH);

        // Generate conditional jump to then or else.
        generateCondition(condition, thenBranch, elseBranch);

        // Generate then (true) branch
        emit(new Label(thenBranch));
        generator.generateStatements(branch.thenStatements);
        generateCondition(assertion, endOfIf, failedAssertionLabel); // Go to failed assertion if expression is false.

        // Generate else (false) branch.
        emit(new Label(elseBranch));
        generator.generateStatements(branch.elseStatements);
        generateCondition(assertion, failedAssertionLabel, endOfIf); // Go to failed assertion if expression is true.

        // Join the execution.
        emit(new Label(endOfIf));
    }

    /**
     * Generates code for a reversible loop statement.
     * <p>
     * The first code generated for the {@link LoopStatement} is the entry {@link Expression}, which is
     * passed as a parameter for this method. Only if the entry condition evaluates to <code>true</code>,
     * the loop is performed.
     * </p>
     * <p>
     * This is done by placing a label below the entry condition before generating the first body.
     * If the entry condition evaluates to <code>true</code> a jump to the first bodies label is performed,
     * starting the loop.
     * </p>
     * <p>
     * At the end of the first body, the exit condition is generated and checked. If the exit condition
     * evaluates to <code>true</code>, a jump out of the loop is made. If it evaluates to <code>false</code>
     * a jump to the second body is made instead.
     * </p>
     * <p>
     * At the end of the second body, the entry condition is generated again. But this time with reversed
     * logic. Since the entry condition must evaluate to <code>false</code> upon repeated evaluation,
     * a jump back to the first body is only made, when the second entry condition evaluates to <code>false</code>.
     * </p>
     * <p>
     * If at any point the condition does not evaluate to the value required to further advance the execution,
     * a jump to an error label is made instead.
     * </p>
     * <p>
     * The control flow is depicted by following diagram:
     * <pre>
     *               |
     *               v
     *          +----+----+ [true]
     *          |  Entry  +----+
     *          |Condition|    |
     *          +---------+    v
     *                     +---+--+
     *       +------------>+First |
     *       |             | Body |
     *       |             +---+--+
     *       |                 |
     *       |                 v
     *       |           +-----+----+ [true]
     *       |       +---+   Exit   +-+
     *       |       |   | Condition| |
     *       |       v   +----------+ |
     *       |   +---+--+             |
     *       |   |Second|             |
     *       |   | Body |             |
     *       |   +---+--+             |
     *       |       |                |
     *       |       v                |
     *       |  +----+----+           |
     *       +--+ ¬Entry  |           |
     *   [true] |Condition|           |
     *          +---------+           v
     * </pre>
     * </p>
     *
     * @param generator      The generator used to generate code for both bodies.
     * @param entryCondition The entry condition that must evaluate to <code>true</code> on entry of the loop
     *                       and <code>false</code> on every repeated evaluation.
     * @param firstBody      The first body which is executed before checking the exit condition.
     * @param exitCondition  The exit condition which is checked to exit the loop. If it evaluates to
     *                       <code>true</code> the loop is exited.
     * @param secondBody     The second body which is executed after checking the exit condition.
     */
    public void generateReversibleLoop(StatementTacGenerator generator,
                                       Expression entryCondition, List<Statement> firstBody,
                                       Expression exitCondition, List<Statement> secondBody) {
        String firstLoopBody = newLabel();
        String secondLoopBody = newLabel();
        String endOfLoop = newLabel();

        // Check entry condition.
        String entryErrorLabel = failedAssertionLabel(RuntimeAssertion.ENTRY_CONDITION_MUST_BE_TRUE);
        generateCondition(entryCondition, firstLoopBody, entryErrorLabel);

        // Generate first body code.
        emit(new Label(firstLoopBody));
        generator.generateStatements(firstBody);
        // If exit condition is true, jump out of the loop.
        generateCondition(exitCondition, endOfLoop, secondLoopBody);

        // Generate second body code.
        emit(new Label(secondLoopBody));
        generator.generateStatements(secondBody);
        // Entry condition must be false to loop.
        String reentryErrorLabel = failedAssertionLabel(RuntimeAssertion.ENTRY_CONDITION_MUST_BE_FALSE);
        generateCondition(entryCondition, reentryErrorLabel, firstLoopBody);

        emit(new Label(endOfLoop));
    }

    /**
     * Generates the code for a {@link LocalBlockStatement}.
     * <p>
     * The variable introduced by this statement is initialized with the value of the
     * first {@link Expression} passed to this method.
     * </p>
     * <p>
     * With the initialized variable the {@link Environment} is prepared for the generation
     * of {@link LocalBlockStatement#body}. The code for the statements in the body are
     * generated using the {@link StatementTacGenerator} provided as the argument.
     * </p>
     * <p>
     * After the body has been generated, the assertion of the end of the {@link LocalBlockStatement}
     * is generated. This assertion checks, that the value of the introduced variable matches
     * the value of the {@link Expression} passed to this method as the de-initializer.
     * </p>
     *
     * @param generator       The generator used to generate code for the statement's body.
     * @param variable        The variable introduced by the statement.
     * @param initializer     The expression used to initialize the new variable.
     * @param deininitializer The expression used for the assertion.
     * @param body            The body in which the variable is valid.
     */
    public void generateLocalBlock(StatementTacGenerator generator, VariableEntry variable,
                                   Expression initializer, Expression deininitializer, List<Statement> body) {
        // Initialize local variable.
        var varAddress = loadVariableAddress(variable);
        var initialValue = generateExpression(initializer);
        if (variable.type == PrimitiveType.IntType ||
                initializer instanceof StackNilLiteral) {
            // Perform a simple assignment on integers or the empty stack.
            emit(new StoreInstruction(varAddress, initialValue));
        } else {
            // Perform complex assignment.
            stackImplementation.generateAssignment(this, varAddress, initialValue);
        }


        // Create new scope for the local variable block.
        var currentScope = new Scope(getVariable(variable));
        parentScopeList.add(currentScope);
        // Save scope list before generating body of local block.
        var savedScopeList = parentScopeList;
        parentScopeList = currentScope.children;

        // Generate body with local variable.
        environment.enter(variable);
        generator.generateStatements(body);
        environment.exit(variable);

        // Restore scope list after body was generated.
        parentScopeList = savedScopeList;

        // Re-evaluate the address since we have no control over the generated code in the body.
        varAddress = loadVariableAddress(variable);
        var actualValue = emitLoad(varAddress);

        // Prepare check of assertion.
        String mismatchLabel = failedAssertionLabel(RuntimeAssertion.INITIALIZER_MISMATCH);
        if (variable.type == PrimitiveType.IntType) {
            // Perform a simple comparison on integers.
            var assertedValue = generateExpression(deininitializer);
            emitBranch(assertedValue, actualValue, IfGotoInstruction.RelativeOperator.NEQ, mismatchLabel);

        } else {
            // Perform complex stack comparison. This requires a fresh label.
            String stackIsCorrectLabel = newLabel();
            if (deininitializer instanceof StackNilLiteral) {
                // Stack should be empty.
                stackImplementation.generateIsEmpty(this, actualValue, stackIsCorrectLabel, mismatchLabel);
            } else {
                // Stack should equal other stack.
                var assertedStack = generateExpression(deininitializer);
                stackImplementation.generateComparison(this, assertedStack, actualValue, stackIsCorrectLabel, mismatchLabel);
            }
            emit(new Label(stackIsCorrectLabel));

            // Re-evaluate the address, since the comparison of stack values could call a method.
            varAddress = loadVariableAddress(variable);
            stackImplementation.generateExit(this, varAddress);
        }
    }

    /**
     * Generates a call to the given procedure name using the list of parameters and arguments.
     * <p>
     * Every argument of the given list is evaluated. The address that the {@link Variable} evaluates to
     * is placed onto the stack. The offset is fetched from the corresponding {@link ParameterType}.
     * </p>
     * <p>
     * After all arguments are evaluated and pushed onto the runtime stack, control is passed to
     * the given procedure using a {@link CallInstruction}.
     * </p>
     *
     * @param calledProcedure The name of the procedure to be called.
     * @param argumentList    The list of arguments passed to the procedure.
     * @param paramTypeList   The list of {@link ParameterType}s used to calculate arguments offsets.
     */
    public void generateCall(String calledProcedure, List<Variable> argumentList, List<ParameterType> paramTypeList) {
        Iterator<Variable> arguments = argumentList.iterator();
        Iterator<ParameterType> parameterTypes = paramTypeList.iterator();

        // Prepare all arguments.
        while (arguments.hasNext()) {
            var argument = generateVariable(arguments.next());
            emit(new ParamInstruction(parameterTypes.next(), argument));
        }
        emit(new CallInstruction(calledProcedure));
    }

    /**
     * Generates code to push the value of a {@link Variable} onto a stack represented by
     * another {@link Variable}.
     * <p>
     * Stack and value of the variable are evaluated before passing the generation of the push instruction
     * off to {@link StackImplementation#generatePush(TacGenerator, TemporaryOperand, TemporaryOperand)}.
     * </p>
     * <p>
     * The variable is zero-cleared after the {@link RuntimeLibrary} code and the stack is updated with the
     * value returned by the library.
     * </p>
     *
     * @param variable The variable to be pushed onto the stack.
     * @param stack    The variable holding the stack. It is zero-cleared afterwards.
     */
    public void generateStackPush(Variable variable, Variable stack) {
        // Fetch stack and variable.
        var pushedVariable = generateVariable(variable);
        var pushedValue = emitLoad(pushedVariable);
        var stackVariable = generateVariable(stack);

        // Let runtime generate push operation.
        stackImplementation.generatePush(this, stackVariable, pushedValue);

        // Zero-clear the variable pushed onto the stack.
        pushedVariable = generateVariable(variable);
        emit(new StoreInstruction(pushedVariable, new ConstantOperand(0)));
    }

    /**
     * Generates code to pop a value from a stack given as a {@link Variable} into another {@link Variable}
     * that must be zero-cleared beforehand.
     * <p>
     * The {@link Variable Variable's} value is loaded and a check is made to ensure it is zero.
     * If this check fails, {@link RuntimeAssertion#POP_NONZERO} will abort the execution.
     * </p>
     * <p>
     * Afterwards the stack is loaded and both, stack and variable, are passed to
     * {@link StackImplementation#generatePop(TacGenerator, TemporaryOperand, TemporaryOperand)} which handles
     * the generation of the pop operation and ensures the stack is not <code>nil</code>.
     * </p>
     * <p>
     * The value returned from the {@link RuntimeLibrary} is used to update the stack.
     * </p>
     *
     * @param variable The zero-cleared variable the value is popped into.
     * @param stack    The stack the value is popped from.
     */
    public void generateStackPop(Variable variable, Variable stack) {
        var poppedVariable = generateVariable(variable);
        var poppedValue = emitLoad(poppedVariable);

        // Ensure that the popped into variable is zero.
        String nonZeroLabel = failedAssertionLabel(RuntimeAssertion.POP_NONZERO);
        emitBranch(poppedValue, new ConstantOperand(0), IfGotoInstruction.RelativeOperator.NEQ, nonZeroLabel);

        var stackVariable = generateVariable(stack);
        stackImplementation.generatePop(this, stackVariable, poppedVariable);
    }


    /**
     * Creates a new {@link TemporaryOperand} and emits a {@link LoadInstruction} loading the value at the given
     * {@link Operand} into the {@link TemporaryOperand}.
     *
     * @param source The memory position to load.
     * @return A new {@link TemporaryOperand} holding the result of the {@link LoadInstruction}.
     */
    public TemporaryOperand emitLoad(Operand source) {
        return emitLoad(source, new Constant(0));
    }

    /**
     * Creates a new {@link TemporaryOperand} and emits a {@link LoadInstruction} loading the value at the given
     * {@link Operand} plus the offset {@link Constant} into the {@link TemporaryOperand}.
     *
     * @param source The memory position to load.
     * @param offset The offset from the memory position.
     * @return A new {@link TemporaryOperand} holding the result of the {@link LoadInstruction}.
     */
    public TemporaryOperand emitLoad(Operand source, Constant offset) {
        var result = newTempOperand();
        emit(new LoadInstruction(result, source, offset));
        return result;
    }

    /**
     * Returns an {@link Operand} containing the address of the given {@link Variable} and emits
     * the code to load the address into the returned {@link Operand}.
     * <p>
     * If the variable is a parameter, which are passed by reference, an additional de-referencing
     * instruction is emitted.
     * </p>
     *
     * @param variable The {@link VariableEntry} of the variable to calculate the address of.
     * @return An {@link Operand} holding the address of the given variable.
     * @implNote In contrast to "Compilerbau, Tl. 2" variables are not simply represented by their "name"
     * in the 3AC. The target architecture in the book (p. 635) has more potent addressing modes than
     * the ECO32 instruction set allows. Since the ECO32 is a possible target architecture for the code
     * generated by this class, those restrictions are represented by the generated code.
     */
    public TemporaryOperand loadVariableAddress(VariableEntry variable) {
        var localAddress = newTempOperand();
        var variableOperand = new NamedOperand(variable, namer.getVariableNameInScope(variable));
        // Use a dedicated AddressOf-Instruction to load the variable address.
        emit(new AddressOfInstruction(localAddress, variableOperand));

        // If the variable is a (reference) parameter, load the address pointing to the original place.
        if (variable.isParameter) {
            return emitLoad(localAddress);
        }
        return localAddress;
    }

    /**
     * Generates code to evaluate a {@link Variable}.
     * <p>
     * The {@link Operand} returned holds the address of memory the {@link Variable} represents.
     * </p>
     *
     * @param variable The {@link Variable} to evaluate.
     * @return The {@link Operand} holding the address of the {@link Variable}.
     */
    public TemporaryOperand generateVariable(Variable variable) {
        setActiveTreeNode(variable);
        return (TemporaryOperand) variable.accept(operandGenerator);
    }

    /**
     * Generates code to evaluate an {@link Expression}.
     * <p>
     * Tje {@link Operand} returned holds the value of the {@link Expression}.
     * </p>
     *
     * @param expression The {@link Expression} to evaluate.
     * @return The {@link Operand} holding the value of the {@link Expression}.
     */
    public Operand generateExpression(Expression expression) {
        setActiveTreeNode(expression);
        return expression.accept(operandGenerator);
    }

    /**
     * Generates code to evaluate a condition.
     * <p>
     * The labels passed to the method represent the targets that the control flow jumps to
     * when the condition evaluates to true or false.
     * </p>
     * <p>
     * Since boolean expressions are converted to control flow {@link Instruction Instructions}
     * no value is returned.
     * </p>
     *
     * @param condition The {@link Expression} to evaluate as a condition.
     * @param onTrue    The label jumped to if the condition evaluates to <code>true</code>.
     * @param onFalse   The label jumped to if the condition evaluates to <code>false</code>.
     */
    public void generateCondition(Expression condition, String onTrue, String onFalse) {
        setActiveTreeNode(condition);

        var conditionGenerator = new ExpressionTacGenerator(this, onTrue, onFalse);
        condition.accept(conditionGenerator);
    }

    /**
     * Generates an {@link IfGotoInstruction} that compares both {@link Operand Operands} with the given
     * {@link IfGotoInstruction.RelativeOperator} and jumps to the given label upon <code>true</code>.
     * <p>
     * This code is a more readable abbreviation for:
     * <pre>
     *   emit(new IfGotoInstruction(lhs, rhs, relativeOperator, label));
     * </pre>
     * </p>
     *
     * @param lhs              The left-hand side of the comparison.
     * @param rhs              The right-hand side of the comparison.
     * @param relativeOperator The operator used to compare both {@link Operand Operands}.
     * @param label            The label used as a target if the comparison yields <code>true</code>.
     */
    public void emitBranch(Operand lhs, Operand rhs, IfGotoInstruction.RelativeOperator relativeOperator, String label) {
        var branchInstruction = new IfGotoInstruction(lhs, rhs, relativeOperator, label);
        emit(branchInstruction);
    }

    /**
     * Generates an {@link ArithmeticInstruction} that combines both {@link Operand Operands} using the
     * given {@link ArithmeticInstruction.Operator}. The {@link TemporaryOperand} holding the result is returned.
     *
     * @param lhs      The left-hand side of the arithmetic instruction.
     * @param rhs      The right-hand side of the arithmetic instruction.
     * @param operator The operator used to arithmetically combine both operands
     * @return The {@link TemporaryOperand} holding the result of the arithmetic operation.
     */
    public TemporaryOperand emitArithmetic(Operand lhs, Operand rhs, ArithmeticInstruction.Operator operator) {
        var resultOperand = newTempOperand();
        emit(new ArithmeticInstruction(resultOperand, lhs, rhs, operator));
        return resultOperand;
    }

    /**
     * Generates a {@link CopyInstruction} that assigns the value of the given {@link Operand}
     * to another {@link TemporaryOperand}.
     *
     * @param toCopy The {@link Operand} to copy.
     * @return The {@link TemporaryOperand} holding a copy of the value.
     */
    public TemporaryOperand emitCopy(Operand toCopy) {
        var resultOperand = newTempOperand();
        emit(new CopyInstruction(resultOperand, toCopy));
        return resultOperand;
    }
}
