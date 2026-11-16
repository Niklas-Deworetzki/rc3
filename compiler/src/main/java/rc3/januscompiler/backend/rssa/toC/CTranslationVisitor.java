package rc3.januscompiler.backend.rssa.toC;

import rc3.januscompiler.Direction;
import rc3.januscompiler.backend.AbstractCNameResolver;
import rc3.januscompiler.backend.AnnotationMode;
import rc3.lib.utils.SetOperations;
import rc3.lib.parsing.Position;
import rc3.rssa.annotations.views.AllocationView;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.InBoundsView;
import rc3.rssa.annotations.views.SourceView;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.RSSAVisitor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class implements the syntax directed translation of RSSA constructs to C.
 * <p>
 * Use of this class is demonstrated in {@link CTranslator}. Every instance of this class should only be
 * used to translate a single {@link BasicBlock}. Every {@link BasicBlock} is translated into a single
 * function, which can be obtained using {@link CTranslationVisitor#toCFunction()}. This {@link CFunction}
 * can be used to acquire the actual translation contents or write them directly to a file.
 * <p>
 * Instances of this class are parameterized with 6 different arguments that configure the translation:
 * <ol>
 *  <li>The {@link ControlGraph} containing the {@link BasicBlock} to translate is required. This
 *  {@link ControlGraph} holds information about input parameters and returned values for the translated procedure,
 *  which is required for translation in all {@link BasicBlock BasicBlocks}.</li>
 *  <li>The {@link BasicBlock} to translate. All {@link Instruction Instructions} of this {@link BasicBlock} are
 *  translated in the order they appear in, forming a {@link CFunction}.</li>
 *  <li>The {@link Direction} tells the translator which of the two execution directions the C translation
 *  should represent. Since C is not reversible, every {@link BasicBlock} has to be translated forwards as well as
 *  backwards to capture the whole semantics.</li>
 *  <li>The {@link AnnotationMode} describes the amount and type of source annotations that should be placed in
 *  the generated code.</li>
 *  <li>A boolean value <var>countExecutedInstructions</var> tells the translator whether to emit additional
 *  C code which count how many instructions are executed during runtime.</li>
 *  <li>A boolean value <var>conditionShouldBeTrue</var> is used to differentiate between the two possible
 *  translations for {@link BasicBlock BasicBlocks} starting with a {@link ConditionalEntry}. This optional
 *  parameter can be omitted if the {@link BasicBlock} to translate starts with a {@link BeginInstruction}
 *  or {@link UnconditionalEntry}.</li>
 * </ol>
 *
 * <h2>Translation scheme</h2>
 * The general translation scheme translates RSSA {@link Value Values} into C R-Values or <i>Expressions</i>.
 * The translation of {@link Variable Variables} yields their variable, which is used in different translations,
 * where a name is required in C.
 * RSSA {@link Instruction Instructions} are translated into one or more C statements. In the case of entry points
 * a function body is started by emitting the functions signature and an opening <code>{</code> bracket.
 * Similarly, exit points end in a closing <code>}</code> bracket.
 * Instead of translating exit and entry points as labels and jumps, we translate them as distinct functions
 * and calls to functions. This has the disadvantage, that jumps to another {@link BasicBlock} introduce a new
 * stack frame without destroying the first one. Therefore loops are essentially implemented as recursion,
 * giving the possibility of stack overflow errors for long running loops with many iterations. The major advantage
 * is, that the introduction of a new function with stack frame removes the necessity of manual variable management.
 * RSSA destroys and creates variables per {@link BasicBlock} which would've been in required in C too, if all
 * {@link BasicBlock BasicBlocks} shared a function.
 * <p>
 * This implementation handles different overheads that are introduced by translating to C as a target language.
 * {@link AbstractCNameResolver#replaceIllegalChars(String)} is used to create valid C identifiers for variables
 * and functions. To ensure the generated names do not collide with either reserved C keywords or names defined
 * in the prelude, the union of two sets {@link CTranslationVisitor#RESERVED_RSSA_PRELUDE_NAMES} and
 * {@link AbstractCNameResolver#RESERVED_KEYWORDS} is used to detect invalid names.
 * If a name is reserved or already taken, a numeric suffix is created to ensure uniqueness.
 * <p>
 * RSSA allows a called procedure to not only accept an arbitrary amount of parameters but also return an arbitrary
 * amount of values, which are all passed by value. Since C does not support the passing of arbitrary large
 * constructs as value, one would have to create some kind of container for the arbitrary amount of result values.
 * Since this container would require explicit memory management as well as explicit packing and unpacking of
 * values, this method seems error prune, complicated and comparatively slow.
 * As an alternative, our implementation introduces implicit parameters to the translated functions. This
 * list of implicit parameters is appended to all regular explicit parameter lists and holds pointers to
 * memory locations, which are used to assign the results of a procedure. When a function is called, temporary
 * variables are allocated on the stack and passed by reference to this implicit parameter list, therefore
 * removing the necessity of explicit memory management, which would require additional error handling.
 * The implicitly extended parameter list is abstracted away using the {@link ImplicitParameterList} class.
 * This class is used to create the actual parameter list required for a C definition, store names of
 * passed parameters (user defined, temporary for constants, as well as implicitly added) and manage
 * the arguments passed when jumping to another procedure.
 * <p>
 * As already mentioned above, exit points of {@link BasicBlock BasicBlocks} are translated to function calls
 * in C. The only exception are {@link EndInstruction}, which assign the result values to the memory location
 * described by the implicitly passed pointers. Therefore to ensure that those pointers reach the
 * {@link EndInstruction} of a function, all entry and exit points have to pass through the implicit parameter
 * list.
 * {@link ConditionalEntry} contains two labels, which is not possible for a single function in C. Therefore
 * two translations of {@link BasicBlock BasicBlocks} starting with an {@link ConditionalEntry} are created:
 * one for each of the labels. Both have a nearly identical body except the verification of the condition in
 * the beginning, which has to be <code>true</code> or <code>false</code> depending on the variant. This
 * behavior is controlled by the <var>conditionShouldBeTrue</var> parameter of this class' constructor.
 * <p>
 * An additional aspect to observe while translating RSSA to C is that a {@link Variable} in RSSA can always
 * (syntactically) be replaced by {@link Constant}. When used as an R-Value in C, this does not introduce any
 * problems. Used as an L-Value or within the parameter list of a function however we would create invalid
 * C code. Therefore a special treatment for these cases is implemented:
 * <ul>
 *  <li>Every {@link Constant} within a parameter list is replaced by a temporary, compiler generated variable.
 *  Additionally to the parameter list, the translator will emit an <code>assert</code> statement in C that
 *  verifies the value passed to this temporary variable as it would be done by a constant in RSSA.</li>
 *  <li>Similarly an assignment to a {@link Constant} is translated as an <code>assert</code> statement,
 *  verifying the assigned value instead of assigning it to a newly declared variable.</li>
 * </ul>
 *
 * <h2>Translation of constructs</h2>
 * <ul>
 *  <li>{@link BinaryOperand} instances are translated to a binary expression in C. The left and right operand
 *  is translated separately and the operator is inserted between them. The translator will always emit
 *  parentheses around the resulting expression.</li>
 *  <li>{@link Constant} instances are translated to their corresponding integer literal.</li>
 *  <li>{@link Variable} instances are translated to a unique and valid C identifier that is used to represent
 *  their value. Used as an L-Value they are represented as a variable declaration, prepending the <code>int</code>
 *  type to the identifier.</li>
 *  <li>{@link MemoryAccess} instances are translated to an array access <code>memory[i]</code> where <var>i</var>
 *  represents the index of the {@link MemoryAccess}.</li>
 *  <li>{@link ArithmeticAssignment} is translated as an assignment or an assertion, depending on the assigned-to
 *  {@link Atom}. In either case a direct translation using C syntax is possible.</li>
 *  <li>{@link BeginInstruction} is translated as the start of a function with additional <code>assert</code>
 *  statements emitted for {@link Constant} parameters.</li>
 *  <li>{@link CallInstruction} requires the most complex translation. It declares temporary variables for the
 *  return values, evaluates the passed parameters, passed both to the called function in the right execution
 *  direction and binds the values returned by the called function. If the bound to {@link Atom} is a
 *  {@link Constant} an <code>assert</code> statement is emitted instead.</li>
 *  <li>{@link ConditionalEntry} is translated as the start of a function. Depending on whether the translation
 *  should create the <i>true</i> or <i>false</i> variant of a {@link ConditionalEntry}, the entries condition
 *  is verified to be the correct value using an <code>assert</code> statement.</li>
 *  <li>{@link ConditionalExit} is translated as an <code>if</code> statement. Both bodies contain a function
 *  call to the function of the succeeding {@link BasicBlock}.</li>
 *  <li>{@link EndInstruction} assigns its parameters to the implicitly acquired parameter list, therefore
 *  placing the return values of a procedure on the callers stack. Additionally the function is closed.</li>
 *  <li>{@link MemoryAssignment} directly translate to a modifying assignment in C to the <var>memory</var>
 *  array.</li>
 *  <li>{@link MemoryInterchangeInstruction} translates to a series of assignments. The memory value is first
 *  either verified or assigned, before it is overwritten in a second assignment.</li>
 *  <li>{@link MemorySwapInstruction} uses a <code>memorySwap</code> macro for its implementation.</li>
 *  <li>{@link SwapInstruction} is implemented as a series of assignments, which swap the values or verify
 *  them using <code>assert</code> statements.</li>
 *  <li>{@link UnconditionalEntry} is translated as the start of a function with accompanying checks for
 *  {@link Constant} parameters.</li>
 *  <li>{@link UnconditionalExit} is translated as a call to the function of the succeeding {@link BasicBlock}</li>
 * </ul>
 *
 * <h2>Additional elements</h2>
 * Additionally this translator emits code for {@link Annotation Annotations}
 * present on the translated {@link Instruction Instructions} and {@link Value Values}. These handle exceptional
 * behavior or states (for example bounds checks) that cannot be expressed in RSSA otherwise.
 * <p>
 * Using the <var>countExecutedInstructions</var> parameter of this class' constructor it is also possible to
 * let the translator emit additional statements and data structures, to count the amount of executed
 * {@link Instruction Instructions} during runtime.
 *
 * @see CFunction The CFunction class provides methods to access or write the implementation of a translated
 * function as well as its prototype.
 */
public class CTranslationVisitor extends RSSAVisitor<String> {
    // Immutable configuration of translation.
    private final BasicBlock blockToTranslate;
    private final Direction translationDirection;
    private final AnnotationMode annotationMode;
    private final boolean countExecutedInstructions;
    private final boolean conditionShouldBeTrue;

    // Implicit parameter list is generated from above information and stored for translation.
    private final ImplicitParameterList implicitParameterList;
    // Function signature is generated from parameter list and entry point.
    private final String functionSignature;

    // Mutable state ensures unique variable names and allows creation of source annotations.
    private long temporaryVariableCount = 0;
    private Position lastPosition = Position.NONE;

    // Target for translation.
    private PrintWriter output;

    /**
     * This {@link Set} holds all identifiers that are used within the Prelude of a translated RSSA program.
     * Therefore they are not available for use as identifiers for user defined variables or functions.
     */
    public static final Set<String> RESERVED_RSSA_PRELUDE_NAMES = Set.of(
            "COUNT_INSTRUCTIONS", "PROCEDURE_COUNT",
            "STOP_RUNTIME", "STOP_RUNTIME_MESSAGE",
            "RSSA_HEAPTOP", "RSSA_HEAPSIZE", "RSSA_AVAILABLE_HEAP",

            "memory", "memorySwap",

            "__instruction_count", "__procedure_names", "__unique_procedure_id",
            "__INCREMENT_INSTRUCTION_COUNTER",

            "__bootstrap_janus_fw",
            "__bootstrap_janus_bw",

            "__check_alloc",
            "__check_free",
            "__check_index",

            "_PRINT_MAIN_MESSAGE",
            "__print_int",
            "__print_stack",
            "__print_array"
    );

    /**
     * This {@link Set} holds the union of identifiers that are reserved by the Prelude or a
     * reserved keyword in C.
     */
    private static final Set<String> DISALLOWED_C_IDENTIFIERS =
            SetOperations.union(RESERVED_RSSA_PRELUDE_NAMES, AbstractCNameResolver.RESERVED_KEYWORDS);


    protected CTranslationVisitor(ControlGraph parentGraph, BasicBlock blockToTranslate,
                                  Direction translationDirection, AnnotationMode annotationMode,
                                  boolean countExecutedInstructions) {
        this(parentGraph, blockToTranslate, translationDirection, annotationMode, countExecutedInstructions, false);
    }

    protected CTranslationVisitor(ControlGraph parentGraph, BasicBlock blockToTranslate,
                                  Direction translationDirection, AnnotationMode annotationMode,
                                  boolean countExecutedInstructions,
                                  boolean conditionShouldBeTrue) {
        this.blockToTranslate = blockToTranslate;
        this.translationDirection = translationDirection;
        this.annotationMode = annotationMode;
        this.countExecutedInstructions = countExecutedInstructions;
        this.conditionShouldBeTrue = conditionShouldBeTrue;

        this.implicitParameterList = initializeParameterNames(parentGraph);
        this.functionSignature = generateFunctionSignature();
    }

    /**
     * Translate the {@link BasicBlock} by translating all {@link Instruction Instructions} within it.
     *
     * @implNote This method is marked <code>synchronized</code> to prevent conflicts, since it modifies
     * the state of this instance.
     */
    private synchronized void doTranslation(PrintWriter output) {
        this.output = output;

        for (Instruction instruction : blockToTranslate) {
            addSourceAnnotation(instruction);
            addHeapAnnotations(instruction);

            if (instruction instanceof ControlInstruction &&
                    ((ControlInstruction) instruction).isEntryPoint()) {
                // Entry points must create a function body before the counter is emitted.
                instruction.accept(this);
                initializeInstructionCounterIfRequested();
                incrementInstructionCounterIfRequested();
            } else {
                incrementInstructionCounterIfRequested();
                instruction.accept(this);
            }
        }
    }

    /**
     * Creates a new {@link CFunction} that is able to produce the function prototype and implementation
     * of the translated {@link BasicBlock}.
     */
    public CFunction toCFunction() {
        return new CFunction(functionSignature + ";", this::doTranslation);
    }

    /**
     * Create a label from a unique label present in an RSSA instruction. This method appends
     * the execution direction to the label to ensure both direction have unique names.
     */
    protected String formatLabel(String label) {
        return String.format("%s_%s", label, translationDirection.asSuffix());
    }

    /**
     * Creates an unique identifier for a variable.
     */
    protected String nextTemporaryVariable() {
        return String.format("_tmp%d", ++temporaryVariableCount);
    }

    /**
     * Generates the function signature for the translated procedure.
     * This is done by inspecting the {@link BasicBlock#entryPoint()} and using the {@link ImplicitParameterList}.
     */
    protected String generateFunctionSignature() {
        ControlInstruction entry = blockToTranslate.entryPoint();
        if (entry instanceof BeginInstruction) {
            return String.format("void %s_%s(%s)", ((BeginInstruction) entry).label, translationDirection.asSuffix(),
                    implicitParameterList.formatInputParameters());
        } else if (entry instanceof UnconditionalEntry) {
            return String.format("void %s(%s)", formatLabel(((UnconditionalEntry) entry).label),
                    implicitParameterList.formatInputParameters());
        } else if (entry instanceof ConditionalEntry) {
            if (conditionShouldBeTrue) {
                return String.format("void %s(%s)", formatLabel(((ConditionalEntry) entry).trueLabel),
                        implicitParameterList.formatInputParameters());
            } else {
                return String.format("void %s(%s)", formatLabel(((ConditionalEntry) entry).falseLabel),
                        implicitParameterList.formatInputParameters());
            }
        } else {
            throw new IllegalStateException(entry.getClass().getCanonicalName() + " is illegal entry point of basic block!");
        }
    }

    /**
     * Creates a new {@link ImplicitParameterList} for this instance.
     */
    private ImplicitParameterList initializeParameterNames(ControlGraph controlGraph) {
        int outputParameters = controlGraph.getEnd().outputParameters().size();
        return new ImplicitParameterList(this, blockToTranslate, outputParameters);
    }

    /**
     * Generate <code>assert</code> statements for every {@link Constant} in the formal parameter list.
     */
    private void generateConstantParameterVerification(List<Atom> formalParameters, List<String> parameterNames) {
        for (int i = 0; i < formalParameters.size(); i++) {
            Atom formalParameter = formalParameters.get(i);

            if (formalParameter instanceof Constant) {
                output.printf("assert(%d == %s);\n",
                        ((Constant) formalParameter).value, parameterNames.get(i));
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Translation of instructions.
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String visit(ArithmeticAssignment instruction) {
        String rightHandSide = String.format("%s %s %s",
                instruction.source.accept(this),
                instruction.operator,
                instruction.getValue().accept(this));

        if (instruction.target instanceof Constant constant) {
            output.printf("assert(%d == (%s));\n", constant.value, rightHandSide);

        } else {
            output.printf("int %s = %s;\n", instruction.target.accept(this), rightHandSide);

        }
        return null;
    }

    @Override
    public String visit(BeginInstruction entry) {
        output.println(functionSignature + "{");
        generateConstantParameterVerification(entry.getParameters(), implicitParameterList.getInputParameterNames());
        return null;
    }

    @Override
    public String visit(CallInstruction instruction) {
        List<String> outputNames = new ArrayList<>();

        // Create temporary variables for output reference parameters.
        for (int i = 0; i < instruction.outputList.size(); i++) {
            String temporaryVariable = nextTemporaryVariable();

            outputNames.add(temporaryVariable);
            output.printf("int %s; ", temporaryVariable);
        }
        output.println();

        // Create complete argument list.
        List<String> arguments = new ArrayList<>();
        for (Atom inputArgument : instruction.inputList) {
            arguments.add(inputArgument.accept(this));
        }
        for (String outputArgumentName : outputNames) {
            arguments.add("&" + outputArgumentName);
        }
        String argumentList = String.join(", ", arguments);

        // Emit call to procedure in correct direction.
        output.printf("%s_%s(%s);\n",
                instruction.procedure, instruction.direction.asSuffix(), argumentList);


        // Bind results or check assertions.
        for (int i = 0; i < instruction.outputList.size(); i++) {
            Atom outputParameter = instruction.outputList.get(i);
            String name = outputNames.get(i);

            if (outputParameter instanceof Constant) {
                output.printf("assert(%d == %s);\n",
                        ((Constant) outputParameter).value, name);
            } else {
                output.printf("int %s = %s;\n",
                        outputParameter.accept(this), name);
            }
        }
        return null;
    }

    @Override
    public String visit(ConditionalEntry entry) {
        output.println(functionSignature + "{");

        if (conditionShouldBeTrue) {
            // Check if condition is TRUE.
            output.printf("assert(0 != (%s));\n",
                    entry.getCondition().accept(this));
        } else {
            // Check if condition is FALSE.
            output.printf("assert(0 == (%s));\n",
                    entry.getCondition().accept(this));
        }

        // Verify formal parameters with constant value.
        generateConstantParameterVerification(entry.getParameters(), implicitParameterList.getInputParameterNames());
        return null;
    }

    @Override
    public String visit(ConditionalExit exit) {
        String parameterList = implicitParameterList.formatOutputArguments();

        // Emit branch to choose target label.
        output.printf("if (%s) %s(%s); else %s(%s);\n",
                exit.getCondition().accept(this),
                formatLabel(exit.trueLabel), parameterList,
                formatLabel(exit.falseLabel), parameterList);

        // Close function for basic block.
        output.println("}");
        return null;
    }

    @Override
    public String visit(EndInstruction exit) {
        // Assign passed output values to reference parameters.
        for (int i = 0; i < exit.getParameters().size(); i++) {
            Atom outputParameter = exit.getParameters().get(i);
            String parameterName = implicitParameterList.getImplicitParameterNames().get(i);

            output.printf("*%s = %s;\n",
                    parameterName, outputParameter.accept(this));
        }

        // Close function for basic block.
        output.println("}");
        return null;
    }

    @Override
    public String visit(MemoryAssignment instruction) {
        output.printf("%s %s= %s;\n",
                instruction.target.accept(this),
                instruction.operator,
                instruction.getValue().accept(this));
        return null;
    }

    @Override
    public String visit(MemoryInterchangeInstruction instruction) {
        String memoryAccess = instruction.memoryAccess.accept(this);
        if (instruction.left instanceof Constant) {
            output.printf("assert(%s == %s); %s = %s;\n",
                    instruction.left.accept(this), memoryAccess,
                    memoryAccess, instruction.right.accept(this));
        } else {
            output.printf("int %s = %s; %s = %s;\n",
                    instruction.left.accept(this), memoryAccess,
                    memoryAccess, instruction.right.accept(this));
        }
        return null;
    }

    @Override
    public String visit(MemorySwapInstruction instruction) {
        output.printf("memorySwap(%s, %s);\n",
                instruction.left.index.accept(this),
                instruction.right.index.accept(this));
        return null;
    }

    @Override
    public String visit(SwapInstruction instruction) {
        if (instruction.targetLeft instanceof Constant) {
            output.printf("assert(%d == (%s)); ",
                    ((Constant) instruction.targetLeft).value, instruction.sourceLeft.accept(this));
        } else {
            output.printf("int %s = %s; ",
                    instruction.targetLeft.accept(this), instruction.sourceLeft.accept(this));
        }

        if (instruction.targetRight instanceof Constant) {
            output.printf("assert(%d == (%s));\n",
                    ((Constant) instruction.targetRight).value, instruction.sourceRight.accept(this));
        } else {
            output.printf("int %s = %s;\n",
                    instruction.targetRight.accept(this), instruction.sourceRight.accept(this));
        }
        return null;
    }

    @Override
    public String visit(UnconditionalEntry entry) {
        output.println(functionSignature + "{");

        // Verify formal parameters with constant value.
        generateConstantParameterVerification(entry.getParameters(), implicitParameterList.getInputParameterNames());
        return null;
    }

    @Override
    public String visit(UnconditionalExit exit) {
        String parameterList = implicitParameterList.formatOutputArguments();
        output.printf("%s(%s);\n",
                formatLabel(exit.label), parameterList);

        // Close function for basic block.
        output.println("}");
        return null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Translation of Operands.
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String visit(BinaryOperand binaryOperand) {
        return String.format("(%s %s %s)",
                binaryOperand.left.accept(this),
                binaryOperand.operator,
                binaryOperand.right.accept(this));
    }

    @Override
    public String visit(Constant constant) {
        return Integer.toString(constant.value);
    }

    @Override
    public String visit(MemoryAccess memoryAccess) {
        return memoryAccess.getBoundsCheck()
                .map(annotation -> String.format("memory[%s]", formatBoundsAnnotation(annotation.getView(), memoryAccess.index)))
                .orElse("");
    }

    @Override
    public String visit(Variable variable) {
        int cycle = 0;
        String proposedName = AbstractCNameResolver.replaceIllegalChars(variable.getRawName());
        if (variable.getVersionNumber() != Variable.UNINITIALIZED) {
            proposedName = String.format("%s_%d", // Concat name and number separately to prevent escaping of underscore.
                    proposedName, variable.getVersionNumber());
        }

        String name = proposedName;
        while (DISALLOWED_C_IDENTIFIERS.contains(name)) {
            name = String.format("%s_%d", proposedName, cycle++);
        }

        return name;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Annotation handling code.
    //////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSourceAnnotation(Instruction instruction) {
        Optional<Position> sourcePosition = instruction.findAnnotation("Source")
                .map(annotation -> (SourceView) annotation.getView())
                .map(SourceView::getPosition);

        if (sourcePosition.isPresent() && sourcePosition.get() != lastPosition) {
            lastPosition = sourcePosition.get();
            output.println(formatPositionAsComment(lastPosition));
        }
    }

    private String formatPositionAsComment(Position position) {
        if (annotationMode == AnnotationMode.NO_ANNOTATIONS) {
            return "";
        }

        Optional<String> sourceLine = position.getSourceLine();
        if (sourceLine.isPresent() && annotationMode == AnnotationMode.SOURCE_ANNOTATIONS) {
            return String.format("// %d: %s", position.getLine(), sourceLine.get());
        }
        return String.format("// %d", position.getLine());
    }

    public void addHeapAnnotations(Instruction instruction) {
        instruction.findAnnotation("Allocation").ifPresent(annotation -> {
            AllocationView view = annotation.getView();
            if (view.requestSize().value < 0) {
                output.printf("__check_free(%s, %d);",
                        view.heapUsage().accept(this), -view.requestSize().value);
            } else {
                output.printf("__check_alloc(%s, %d);",
                        view.heapUsage().accept(this), view.requestSize().value);
            }
        });
    }

    public String formatBoundsAnnotation(InBoundsView view, Atom index) {
        return String.format("__check_index(%s, %s, %s)",
                view.getBaseAddress().accept(this),
                view.getSize(),
                index.accept(this));
    }


    private void incrementInstructionCounterIfRequested() {
        if (!countExecutedInstructions) return;

        output.println("__INCREMENT_INSTRUCTION_COUNTER(__this_procedure_id);");
    }

    private void initializeInstructionCounterIfRequested() {
        if (!countExecutedInstructions) return;

        // An ugly hack to get the raw procedure name...
        String procedureName = functionSignature.substring(
                functionSignature.indexOf(' ') + 1,
                functionSignature.indexOf('('));

        output.println("static int __this_procedure_id = -1;");
        output.println("if (__this_procedure_id == -1) {");
        output.println("\t__this_procedure_id = __unique_procedure_id++;");
        output.println("\t__procedure_names[__this_procedure_id] = \"" + procedureName + "\";");
        output.println("}");
    }
}


