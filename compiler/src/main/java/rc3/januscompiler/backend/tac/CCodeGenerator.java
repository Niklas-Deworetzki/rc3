package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.tac.types.CArrayType;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.tac.instances.*;
import rc3.januscompiler.tac.table.Variable;
import rc3.januscompiler.tac.visitor.DoNothingVoidTacVisitor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Instances of this class are used to convert one {@link CFunction} into actual C code.
 * <p>
 * The temporary variables used in the {@link CFunction CFunction's} body will be declared at most once.
 * If multiple assignments are made to the same variable, it is only declared the first time it is encountered.
 * </p>
 */
public class CCodeGenerator extends DoNothingVoidTacVisitor implements Runnable {

    private static final Map<ArithmeticInstruction.Operator, String> OPERATOR_REPRESENTATION_MAP =
            Map.of(
                    ArithmeticInstruction.Operator.ADD, "+",
                    ArithmeticInstruction.Operator.SUB, "-",
                    ArithmeticInstruction.Operator.MUL, "*",
                    ArithmeticInstruction.Operator.DIV, "/",
                    ArithmeticInstruction.Operator.MOD, "%",
                    ArithmeticInstruction.Operator.AND, "&",
                    ArithmeticInstruction.Operator.OR, "|",
                    ArithmeticInstruction.Operator.XOR, "^");

    private static final Map<IfGotoInstruction.RelativeOperator, String> RELATIVE_OPERATOR_REPRESENTATION_MAP =
            Map.of(
                    IfGotoInstruction.RelativeOperator.EQU, "==",
                    IfGotoInstruction.RelativeOperator.NEQ, "!=",
                    IfGotoInstruction.RelativeOperator.LST, "<",
                    IfGotoInstruction.RelativeOperator.LSE, "<=",
                    IfGotoInstruction.RelativeOperator.U_LST, "< (unsigned int)",
                    IfGotoInstruction.RelativeOperator.U_LSE, "<= (unsigned int)");

    protected final CFunction function;
    protected final CodeEmitter emitter;

    protected final Set<CVariable> declaredTemporaries;

    private final boolean printMain;
    private final boolean countExecutedInstructions;

    /**
     * This array holds the {@link Operand Operands} that are pushed via the {@link ParamInstruction}.
     * The {@link ParameterType#offset} equals the index where to push the instruction.
     */
    protected final Operand[] argumentArea;
    // This array is used when a function does not require an argument area.
    private static final Operand[] EMPTY_ARGUMENT_AREA = new Operand[1 /* One implicit argument may be required for builtin methods. */];


    public CCodeGenerator(CFunction function, CodeEmitter emitter, boolean printMain, boolean countExecutedInstructions) {
        this.function = function;
        this.emitter = emitter;
        this.printMain = printMain;
        this.countExecutedInstructions = countExecutedInstructions;
        this.declaredTemporaries = new HashSet<>(function.temporaryVariables.size());

        var entry = function.procedure.entry;
        if (entry.callsOtherProcedures()) {
            this.argumentArea = new Operand[entry.argumentAreaSize];
        } else {
            this.argumentArea = EMPTY_ARGUMENT_AREA;
        }
    }

    private String getOriginalSignature(ProcedureEntry entry) {
        String parameterList = entry.parameterTypes.stream()
                .map(parameterType -> parameterType.type.toString())
                .collect(Collectors.joining(", "));
        return String.format("%s(%s)", entry.getName(), parameterList);
    }

    @Override
    public void run() {
        emitter.emitNewLine();
        for (Instruction instruction : function.body) {
            if (instruction.hasAnnotation()) {
                emitter.emitComment(instruction.getAnnotation());
            }

            if (instruction instanceof BeginInstruction) {
                instruction.accept(this);
                initializeInstructionCounterIfRequested();
                incrementInstructionCounterIfRequested();
            } else {
                incrementInstructionCounterIfRequested();
                instruction.accept(this);
            }
        }
    }

    protected String declarationIfRequired(TemporaryOperand temporaryOperand) {
        CVariable temporaryVariable = function.findTemporaryVariable(temporaryOperand.name).orElseThrow();
        if (declaredTemporaries.contains(temporaryVariable)) {
            return temporaryVariable.name();
        } else {
            declaredTemporaries.add(temporaryVariable);
            return temporaryVariable.getDeclarationString();
        }
    }


    @Override
    public void visitVoid(AddressOfInstruction instruction) {
        CVariable variable = function.findVariable(instruction.variable.name).orElseThrow();

        if (variable.type() instanceof CArrayType) {
            // Result is address of element at index 0.
            emitter.emitAssignment(
                    declarationIfRequired(instruction.target),
                    "&", instruction.variable.toString() + "[0]");
        } else {
            emitter.emitAssignment(
                    declarationIfRequired(instruction.target),
                    "&", instruction.variable.toString());
        }
    }


    @Override
    public void visitVoid(ArithmeticInstruction instruction) {
        emitter.emitAssignment(
                declarationIfRequired(instruction.target),
                instruction.lhs.toString(),
                OPERATOR_REPRESENTATION_MAP.get(instruction.operator),
                instruction.rhs.toString());
    }

    @Override
    public void visitVoid(BeginInstruction instruction) {
        emitter.emitComment(getOriginalSignature(function.procedure.entry));
        emitter.emitLine(function.getSignatureString() + " {");

        for (CVariable localVariable : function.localVariables) {
            emitter.emitDeclaration(localVariable, localVariable.type().getInitialValue());
        }
        emitter.emitComment("Temporary variables: %d", function.temporaryVariables.size());
    }

    @Override
    public void visitVoid(CallInstruction instruction) {
        Stream<String> arguments = Arrays.stream(argumentArea)
                .filter(Objects::nonNull)
                .map(String::valueOf);
        emitter.emitCall(instruction.label, arguments);
        Arrays.fill(argumentArea, null);
    }

    @Override
    public void visitVoid(CopyInstruction instruction) {
        emitter.emitAssignment(
                declarationIfRequired(instruction.target),
                instruction.source.toString());
    }

    @Override
    public void visitVoid(EndInstruction instruction) {
        if (printMain && function.isJanusMain()) {
            for (Variable mainVariable : function.getLocalDeclarations()) {
                emitter.printDeclaredVariable(
                        function.findLocalVariable(mainVariable.name).orElseThrow(),
                        mainVariable.entry.getStringName()); // Use the original name when printing the variable.
            }
        }
    }

    @Override
    public void visitVoid(GotoInstruction instruction) {
        if (!function.containsLabel(instruction.label)) {
            emitter.emitCall(instruction.label, Stream.empty());
        } else {
            emitter.emitGoto(instruction.label);
        }
    }


    @Override
    public void visitVoid(IfGotoInstruction instruction) {
        emitter.emitCondition(
                instruction.lhs.toString(),
                RELATIVE_OPERATOR_REPRESENTATION_MAP.get(instruction.relativeOperator),
                instruction.rhs.toString());
        emitter.emit("\t");
        if (!function.containsLabel(instruction.label)) {
            emitter.emitCall(instruction.label, Stream.empty());
        } else {
            emitter.emitGoto(instruction.label);
        }
    }

    @Override
    public void visitVoid(Label label) {
        emitter.emitLine(label.name + ": ;");
    }

    @Override
    public void visitVoid(LoadInstruction instruction) {
        if (instruction.offset.getRepresentation().startsWith("->")) {
            emitter.emitMemberLoad(
                    declarationIfRequired(instruction.target),
                    instruction.source.toString(),
                    instruction.offset.getRepresentation());
        } else if (instruction.offset.hasKnownIntValue() &&
                instruction.offset.getKnownIntValue() == 0) {
            emitter.emitLoad(
                    declarationIfRequired(instruction.target),
                    instruction.source.toString());
        } else {
            emitter.emitLoad(
                    declarationIfRequired(instruction.target),
                    instruction.source.toString(),
                    instruction.offset.getRepresentation());
        }
    }

    @Override
    public void visitVoid(ParamInstruction instruction) {
        argumentArea[instruction.parameterType.offset] = instruction.operand;
    }

    @Override
    public void visitVoid(ReturnInstruction instruction) {
        // C automatically destroys the stack frame at the end of a function.
        emitter.emitLine("return;");
        emitter.emitLine("}");
    }

    @Override
    public void visitVoid(StoreInstruction instruction) {
        if (instruction.offset.getRepresentation().startsWith("->")) {
            emitter.emitMemberStore(
                    instruction.target.toString(),
                    instruction.offset.getRepresentation(),
                    instruction.source.toString());
        } else if (instruction.offset.hasKnownIntValue() &&
                instruction.offset.getKnownIntValue() == 0) {
            emitter.emitStore(
                    instruction.target.toString(),
                    instruction.source.toString());
        } else {
            emitter.emitStore(
                    instruction.target.toString(),
                    instruction.offset.getRepresentation(),
                    instruction.source.toString());
        }
    }


    private void incrementInstructionCounterIfRequested() {
        if (!countExecutedInstructions) return;

        emitter.emitLine("__INCREMENT_INSTRUCTION_COUNTER(__this_procedure_id);");
    }

    private void initializeInstructionCounterIfRequested() {
        if (!countExecutedInstructions) return;

        emitter.emitLine("static int __this_procedure_id = -1;");
        emitter.emitLine("if (__this_procedure_id == -1) {");
        emitter.emitLine("\t__this_procedure_id = __unique_procedure_id++;");
        emitter.emitLine("\t__procedure_names[__this_procedure_id] = \"%s\";", function.name);
        emitter.emitLine("}");
    }

}
