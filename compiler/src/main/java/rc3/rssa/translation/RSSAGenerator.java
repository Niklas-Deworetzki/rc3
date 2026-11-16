package rc3.rssa.translation;

import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.syntax.visitor.VoidTreeVisitor;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.types.PrimitiveType;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.parsing.Position;
import rc3.lib.utils.CollectionUtils;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.blocks.ControlGraphGenerator;
import rc3.rssa.blocks.ParameterListAdjuster;
import rc3.rssa.blocks.VariableIndexer;
import rc3.rssa.instances.Variable;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Translates a {@link ProcedureDeclaration} into a list of RSSA-{@link Instruction} objects.
 * <p>
 * The translation unit is available via the {@link RSSAGenerator#generateProcedureCode(ProcedureDeclaration)} method.
 * </p>
 */
public class RSSAGenerator extends DoNothingVoidTreeVisitor {
    // Cached instances.
    private final RSSANameResolver namer = new RSSANameResolver();
    private final TreeVisitor<Value> expressionGenerator = new ExpressionRSSAGenerator(this);
    private final VoidTreeVisitor statementGenerator;

    // Internal state.
    private final List<Instruction> generatedInstructions = new ArrayList<>();
    private final Deque<Instruction> destructionStack = new ArrayDeque<>();
    private final MemoryManagement memoryManagement;
    private final Environment environment;

    // Utility variables to generate reverse code.
    private final Deque<List<Instruction>> temporarySinks = new ArrayDeque<>(4);
    private int openSinks = 0;

    // Counter to generate unique labels.
    private int nextFreeLabel = 0;

    // Annotation management.
    private Position translationPosition = Position.NONE;
    private final HeapSpaceAllocator heapSpaceAllocator;

    public RSSAGenerator(Function<RSSAGenerator, ? extends VoidTreeVisitor> statementGenCtor,
                         MemoryManagement memoryManagement, Environment globalEnvironment) {
        this.memoryManagement = memoryManagement;
        this.environment = globalEnvironment;
        this.statementGenerator = statementGenCtor.apply(this);

        this.heapSpaceAllocator = new HeapSpaceAllocator(memoryManagement);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Management of internal state.
    ////////////////////////////////////////////////////////////////////////////

    public MemoryManagement getMemoryManagement() {
        return memoryManagement;
    }

    public List<Instruction> getGeneratedInstructions() {
        return generatedInstructions;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    /**
     * Reset the internal state for a new translation.
     */
    private void resetState(ProcedureEntry nextProcedure) {
        this.nextFreeLabel = 0;
        this.generatedInstructions.clear();
        this.namer.initializeScope(nextProcedure);
        this.translationPosition = Position.NONE;
    }

    private static Annotation newSourceAnnotation(Position position) {
        return new Annotation("Source", Map.of(
                "position", position
        ));
    }

    private void addPositionAnnotation(Instruction instruction) {
        instruction.annotate(newSourceAnnotation(translationPosition));
    }

    private void updatePosition(Position position) {
        if (translationPosition != position) {
            translationPosition = position;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Management of code generation.
    ////////////////////////////////////////////////////////////////////////////

    private List<Annotation> generateGlobalAnnotationList() {
        // Global annotations to initialize heap for execution.
        List<Annotation> globalAnnotations = new ArrayList<>();
        heapSpaceAllocator.initializeHeapWithAnnotations(globalAnnotations);

        // Mark main procedure as entry point for program.
        globalAnnotations.add(new Annotation("Entry", Map.of(
                "value", MainProcedureDeclaration.NAME)));

        return globalAnnotations;
    }

    public static rc3.rssa.instances.Program generateProgramCode(
            Function<RSSAGenerator, ? extends VoidTreeVisitor> statementGenCtor,
            Program program, MemoryManagement memoryManagement, Environment globalEnvironment) {

        Map<String, List<Instruction>> translatedProcedures = new HashMap<>();
        RSSAGenerator generator = new RSSAGenerator(statementGenCtor, memoryManagement, globalEnvironment);

        for (ProcedureDeclaration procedure : program.procedures) {
            translatedProcedures.put(procedure.name.name, generator.generateProcedureCode(procedure));
        }

        rc3.rssa.instances.Program translatedProgram = new rc3.rssa.instances.Program(
                generator.generateGlobalAnnotationList(),
                ControlGraphGenerator.fromProcedures(translatedProcedures));
        ParameterListAdjuster.adjustParameterLists(translatedProgram);
        VariableIndexer.indexVariables(translatedProgram);
        return translatedProgram;
    }

    /**
     * Translates a {@link ProcedureDeclaration} to a {@link List} of RSSA-{@link Instruction} objects.
     */
    public List<Instruction> generateProcedureCode(ProcedureDeclaration declaration) {
        resetState(declaration.entry); // Clean up before translation.
        declaration.accept(this);

        return new ArrayList<>(generatedInstructions);
    }

    private Variable[] createParameterArray(List<VariableDeclaration> declarationList) {
        List<Variable> parameters = new ArrayList<>();
        for (VariableDeclaration declaration : declarationList) {
            getEnvironment().enter(declaration.entry);

            if (declaration.type == PrimitiveType.StackType) {
                StackVariable stackVariable = new StackVariable(this, declaration.name);

                parameters.add(stackVariable.getStackReference());
                parameters.add(stackVariable.getStackTop());
            } else {
                parameters.add(fromName(declaration.name));
            }
        }

        return parameters.toArray(Variable[]::new);
    }

    @Override
    public void visitVoid(ProcedureDeclaration procedureDeclaration) {
        String procedureName = procedureDeclaration.name.toString();

        emitWithParameters(new BeginInstruction(procedureName), createParameterArray(procedureDeclaration.parameters))
                .annotate(newSourceAnnotation(procedureDeclaration.position));
        {
            procedureDeclaration.body.forEach(this::translateStatement);
        }
        emitWithParameters(new EndInstruction(procedureName), createParameterArray(procedureDeclaration.parameters))
                .annotate(newSourceAnnotation(procedureDeclaration.position));
        getEnvironment().clearLocalVariables();
    }

    @Override
    public void visitVoid(MainProcedureDeclaration mainProcedure) {
        // Variables declared within the main procedure are converted to parameters for the main procedure.
        // But they have to be allocated on the heap and marked with corresponding annotations.
        Variable[] mainProcedureVariables = createParameterArray(mainProcedure.variables);
        heapSpaceAllocator.annotateMainParameters(mainProcedure.variables, mainProcedureVariables);

        emitWithParameters(new BeginInstruction(MainProcedureDeclaration.NAME), mainProcedureVariables)
                .annotate(newSourceAnnotation(mainProcedure.position));
        {
            mainProcedure.body.forEach(this::translateStatement);
        }
        emitWithParameters(new EndInstruction(MainProcedureDeclaration.NAME), mainProcedureVariables)
                .annotate(newSourceAnnotation(mainProcedure.position));
        getEnvironment().clearLocalVariables();
    }

    public void translateStatement(Statement statement) {
        updatePosition(statement.position);
        statement.accept(statementGenerator);
    }

    ////////////////////////////////////////////////////////////////////////////
    // General methods.
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the next free label for the current translation.
     */
    public String generateLabel() {
        return namer.getLabelName(++nextFreeLabel);
    }

    /**
     * Emits a single {@link Instruction} as part of the generated code.
     */
    public void emit(Instruction instruction, Annotation... annotations) {
        addPositionAnnotation(instruction);
        for (Annotation annotation : annotations) {
            instruction.annotate(annotation);
        }

        if (openSinks > 0) {
            temporarySinks.getLast().add(instruction);
        } else {
            generatedInstructions.add(instruction);
        }
    }

    public Value translateExpression(Expression expression) {
        return expression.accept(expressionGenerator);
    }

    public void enqueueDestruction(Instruction... instruction) {
        for (int i = instruction.length - 1; i >= 0; i--) {
            addPositionAnnotation(instruction[i]);
            destructionStack.push(instruction[i]);
        }
    }

    public void emitPendingDestruction() {
        while (!destructionStack.isEmpty()) {
            emit(destructionStack.pop());
        }
    }

    public Deque<Instruction> clearDestructionStack() {
        Deque<Instruction> result = new ArrayDeque<>(destructionStack);
        destructionStack.clear();
        return result;
    }

    public void emitDestructionStack(Deque<Instruction> stack) {
        Deque<Instruction> copy = new ArrayDeque<>(stack);
        while (!copy.isEmpty()) {
            emit(copy.pop().copy());
        }
    }

    public void emitReversed(Runnable codeGenerator) {
        // Open new sink to hold generated code.
        temporarySinks.push(new ArrayList<>());
        openSinks++;

        codeGenerator.run();

        // Empty contents of sink in reversed order.
        openSinks--;
        for (Instruction instruction : CollectionUtils.reverse(temporarySinks.pop())) {
            generatedInstructions.add(instruction.reverse());
        }
    }

    public <T> T emitBuffered(List<Instruction> buffer, Supplier<T> codeGenerator) {
        temporarySinks.push(buffer);
        openSinks++;

        final T result = codeGenerator.get();

        openSinks--;
        temporarySinks.pop();

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Specialized instructions.
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Emits a single assignment without destroying the given value or generating destruction instructions.
     * <pre>
     *   Result := 0 + Value
     * </pre>
     */
    public Variable emitCopyAssignment(Value value) {
        Variable target = Variable.nextTemporaryVariable();
        // Target := 0 ^ Value
        emit(ArithmeticAssignment.newInstruction(
                target, new Constant(0), AssignStatement.ModificationOperator.XOR,
                value));
        return target;
    }

    /**
     * Emits a single assignment without destroying the given value. Destruction instructions are generated.
     * <pre>
     *  Result := 0 ^ Value
     * </pre>
     * <p>
     * Additionally a destruction is pushed onto the stack.
     * <pre>
     *  0 := Result ^ Value
     * </pre>
     */
    public Variable emitDestructibleCopyAssignment(Value value) {
        Variable target = emitCopyAssignment(value);

        // 0 := target ^ value
        enqueueDestruction(ArithmeticAssignment.newInstruction(
                new Constant(0), target, AssignStatement.ModificationOperator.XOR,
                value));

        return target;
    }

    public ControlInstruction emitWithParameters(ControlInstruction controlInstruction, Atom... parameters) {
        emit(controlInstruction);
        controlInstruction.addParameters(Arrays.asList(parameters));
        return controlInstruction;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Value promotion and conversion methods.
    ////////////////////////////////////////////////////////////////////////////

    public Atom promoteToAtom(Value value) {
        if (value instanceof Atom) {
            return (Atom) value;
        } else {
            return emitDestructibleCopyAssignment(value);
        }
    }

    public RValue promoteToRValue(Value value) {
        if (value instanceof RValue) {
            return (RValue) value;
        } else {
            return emitDestructibleCopyAssignment(value);
        }
    }

    public <L extends RValue, R extends RValue> BinaryOperand promoteToBinary(Value value,
                                                                              Function<Value, L> leftPromoter,
                                                                              Function<Value, R> rightPromoter,
                                                                              BinaryOperand.BinaryOperator defaultOperator) {
        if (value instanceof BinaryOperand binary) {
            L left = leftPromoter.apply(binary.left);
            R right = rightPromoter.apply(binary.right);

            return new BinaryOperand(left, binary.operator, right);
        } else {
            Constant identity = new Constant(defaultOperator.getIdentity());

            L left = leftPromoter.apply(value);
            R right = rightPromoter.apply(identity);

            return new BinaryOperand(left, defaultOperator, right);
        }
    }

    public BinaryOperand promoteForMemoryAssignment(Value value) {
        return promoteToBinary(value, this::promoteToAtom, this::promoteToAtom, BinaryOperand.BinaryOperator.XOR);
    }

    public BinaryOperand promoteForArithmeticAssignment(Value value) {
        return promoteToBinary(value, this::promoteToRValue, this::promoteToRValue, BinaryOperand.BinaryOperator.XOR);
    }

    public BinaryOperand promoteForConditional(Value value) {
        return promoteToBinary(value, this::promoteToRValue, this::promoteToRValue, BinaryOperand.BinaryOperator.NEQ);
    }


    ////////////////////////////////////////////////////////////////////////////
    // Variable conversions.
    ////////////////////////////////////////////////////////////////////////////

    public Variable fromNamedVariable(NamedVariable variable) {
        return (Variable) variable.accept(expressionGenerator);
    }

    public MemoryAccess fromIndexedVariable(IndexedVariable variable) {
        return (MemoryAccess) variable.accept(expressionGenerator);
    }

    public Variable fromName(Identifier name) {
        VariableEntry entry = environment.getVariable(name)
                .orElseThrow(() -> new ErrorMessage.InternalError("Encountered unknown variable named '" + name + "'!"));

        return new Variable(namer.getVariableNameInScope(entry));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Memory management methods.
    ////////////////////////////////////////////////////////////////////////////

    private static Annotation newAllocationAnnotation(int size, MemoryManagement memoryManagement) {
        return new Annotation("Allocation", Map.of(
                "usage", memoryManagement.getHeapTop(),
                "request", new Constant(size)));
    }

    public void alloc(Variable variable, int size) {
        final Constant sizeConstant = new Constant(size);

        // HeapTop += Size
        emit(new MemoryAssignment(
                        memoryManagement.getHeapTop(), AssignStatement.ModificationOperator.ADD, BinaryOperand.of(sizeConstant)),
                newAllocationAnnotation(size, memoryManagement));
        // Variable := 0 ^ (HeapTop - Size)
        emit(new ArithmeticAssignment(
                variable, new Constant(0), AssignStatement.ModificationOperator.XOR,
                new BinaryOperand(memoryManagement.getHeapTop(), BinaryOperand.BinaryOperator.SUB, sizeConstant)));
    }

    public void free(Variable variable, int size) {
        emitReversed(() -> alloc(variable, size));
    }
}
