package rc3.rssa.inlining;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.januscompiler.syntax.AssignStatement;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.optimization.Optimization;
import rc3.lib.utils.CollectionUtils;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.blocks.ControlGraphGenerator;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.DoNothingRSSAVisitor;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rc3.lib.utils.CollectionUtils.*;
import static rc3.lib.utils.SetOperations.concat;

/**
 * This class implements the <i>Inlining</i> optimization pass for RSSA programs.
 * <p>
 * Inlining is an optimization that aims to reduce the overhead of calling procedures by replacing relatively slow
 * call instructions by the code of the called procedure. While the concept seems straightforward, too much inlining
 * will vastly increase code size and may thereby harm performance.
 * <p>
 * The decision which calls should be inlined in which order is done by a {@link InliningStrategy} that can be passed
 * to the constructor of this class. The default strategy used is the {@link DefaultInliningStrategy}.
 * <p>
 * RSSA makes inlining more challenging than it is in regular intermediate representations, as the inlined code
 * can not only be called forwards but also backwards. In this case, instructions have to be copied in reverse
 * order with inverse semantics. </br>
 * Additionally RSSA requires explicit parameter passing for values shared between basic blocks, which enforces
 * the need to manipulate inlined parameter lists of entry and exit points, as the inlined code will have to
 * "pass-through" variables that are defined before, but are used after the call is made.
 */
public final class Inlining extends Optimization {
    private final Program originalProgram;
    private final Map<String, List<Instruction>> procedureCode = new HashMap<>();
    private final InliningStrategy inliningStrategy;

    // This instance is shared among different sub-tasks, as they require unique identifiers for labels, variables, etc.
    private final AtomicLong uniqueId = new AtomicLong();

    private final boolean isDebugEnabled;

    public Inlining(Compiler compiler, Program program, InliningStrategy inliningStrategy) {
        super(AvailableOptimization.INLINING, compiler.getOptimizationState());
        this.originalProgram = program;
        this.isDebugEnabled = compiler.getOptimizationState().isDebugModeEnabled(AvailableOptimization.INLINING);

        for (Map.Entry<String, ControlGraph> entry : program.procedures().entrySet()) {
            procedureCode.put(entry.getKey(), entry.getValue().toCode());
        }

        inliningStrategy.initializeStrategy(compiler.getOptimizationParameters(AvailableOptimization.INLINING));
        this.inliningStrategy = inliningStrategy;
        if (isDebugEnabled) {
            inliningStrategy.dumpConfiguration(System.out);
        }
    }

    public Inlining(Compiler compiler, Program program) {
        this(compiler, program, new DefaultInliningStrategy(program));
    }

    // Print an information message for inlining sites if debug is enabled.
    private void debugSite(InliningStrategy.InliningSite site) {
        if (isDebugEnabled) {
            System.out.println("Performing inlining on procedure '" + site.getProcedureName() + "':");

            int index = 1;
            for (CallInstruction inlineableInstruction : site.inlineableInstructions) {
                System.out.printf(" %d\t%s %s%n", index++, inlineableInstruction.direction, inlineableInstruction.procedure);
            }
        }
    }

    /**
     * Performs the inlining optimization.
     * <p>
     * All {@link InliningStrategy.InliningSite InliningSites} are processed in the order, they are presented by
     * this instances {@link InliningStrategy}.
     *
     * @return An optimized {@link Program} in which all sites have been inlined.
     */
    public Program performOptimization() {
        // Perform inlining as queued by strategy.
        for (InliningStrategy.InliningSite inliningSite : inliningStrategy.getInliningQueue()) {
            debugSite(inliningSite);
            procedureCode.put(inliningSite.procedureName, processInliningSite(inliningSite));
        }

        // Transform map into valid format for TranslatedProgram.
        Map<String, List<Instruction>> resultProgram = new HashMap<>();
        for (String procedureName : originalProgram.procedureNames()) {
            resultProgram.put(procedureName, procedureCode.get(procedureName));
        }

        // Return new TranslatedProgram.
        return new Program(originalProgram.globalAnnotations(), ControlGraphGenerator.fromProcedures(resultProgram));
    }

    /**
     * Returns the instructions represented by a procedure call.
     * <p>
     * If the call direction is {@link Direction#BACKWARD} the instructions of the called procedure are
     * returned in reversed order with inverted semantics.
     *
     * @param callInstruction The {@link Instruction} to unfold.
     * @implNote This method returns a <b>copy</b> of the called procedures instructions (reversed or not)
     * and thus allows for modifications of the instructions, without changing the original procedure.
     */
    private List<Instruction> unfoldCall(CallInstruction callInstruction) {
        Stream<Instruction> calledCodeStream = procedureCode.get(callInstruction.procedure).stream();

        if (callInstruction.direction == Direction.BACKWARD) {
            return calledCodeStream
                    .map(Instruction::reverse)
                    .collect(CollectionUtils.collectReversedList());
        } else {
            return calledCodeStream
                    .map(Instruction::copy)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Removes all associated {@link Annotation Annotations} from
     * a {@link Atom parameter}. The {@link Atom} is copied before removing the annotations, so this
     * method won't interfere with other uses of the {@link Atom}.
     */
    private static Atom stripParameterAnnotations(Atom parameter) {
        Atom stripped = parameter.copy();
        stripped.clearAnnotations();
        return stripped;
    }

    /**
     * Generates explicit assignments that are used to replace the implicit name binding of passing parameters.
     */
    private void assignParameters(List<Instruction> resultContainerReference, List<Atom> sourceList, List<Atom> targetList) {
        sourceList = sourceList.stream()
                .map(Inlining::stripParameterAnnotations)
                .collect(Collectors.toList());
        targetList = targetList.stream()
                .map(Inlining::stripParameterAnnotations)
                .collect(Collectors.toList());

        // Note: sourceList and targetList must have the same size
        for (int i = 0; i < sourceList.size(); i++) {
            if (i == sourceList.size() - 1) {
                resultContainerReference.add(new ArithmeticAssignment(targetList.get(i), sourceList.get(i),
                        AssignStatement.ModificationOperator.XOR, new Constant(0),
                        BinaryOperand.BinaryOperator.XOR, new Constant(0)));
            } else {
                resultContainerReference.add(new SwapInstruction(targetList.get(i), targetList.get(i + 1),
                        sourceList.get(i), sourceList.get(i + 1)));
                i++;
            }
        }
    }

    /**
     * Inlines a single {@link CallInstruction} by generating all code necessary to replace this call.
     * <p>
     * The {@link List} of {@link Instruction Instructions} returned by this method contains:
     * <ul>
     *  <li>Assignments to explicitly "pass" the parameters for the called procedure.</li>
     *  <li>The body of the called procedure in correct order with correct directional semantics.</li>
     *  <li>Assignments to explicitly "pass" the result of the called procedure.</li>
     * </ul>
     * <p>
     * All {@link Instruction Instructions} returned by this method are processed to ensure, {@link Variable}
     * names and labels are unique and won't clash with any other names.
     */
    private List<Instruction> inlineCall(CallInstruction callInstruction) {
        List<Instruction> calledInstructions = unfoldCall(callInstruction);
        // Ensure unique Variable names.
        calledInstructions.forEach(new UniqueVariableRenamer(uniqueId));

        BeginInstruction begin = (BeginInstruction) calledInstructions.get(0);
        EndInstruction end = (EndInstruction) calledInstructions.get(calledInstructions.size() - 1);
        List<Instruction> inlinedBody = new ArrayList<>((calledInstructions.size() - 2) +
                callInstruction.inputList.size() + callInstruction.outputList.size());

        // Assign incoming parameters.
        assignParameters(inlinedBody, callInstruction.inputList, begin.getParameters());
        // Copy inlined body while making labels unique.
        final var renameLabels = new UniqueLabelRenamer(uniqueId).asFunction();
        for (Instruction instruction : subList(calledInstructions, 1, 1)) {
            inlinedBody.add(renameLabels.apply(instruction));
        }
        // Assign outgoing parameters.
        assignParameters(inlinedBody, end.getParameters(), callInstruction.outputList);
        return inlinedBody;
    }

    /**
     * Processes a single {@link InliningStrategy.InliningSite} by iterating the caller procedures body and
     * inlining all calls that are scheduled by the {@link InliningStrategy.InliningSite}.
     * <p>
     * This method keeps track of live {@link Variable Variables} within the procedure as they have to be
     * passed through inlined code.
     */
    public List<Instruction> processInliningSite(InliningStrategy.InliningSite site) {
        final List<Instruction> result = new ArrayList<>();
        final Set<Variable> activeVariables = new HashSet<>();
        final Map<Variable, Variable> renamedVariables = new HashMap<>();

        for (Instruction instruction : procedureCode.get(site.getProcedureName())) {
            List<Variable> destroyed = instruction.variablesDestroyed();

            for (Variable v : concat(instruction.variablesUsedOrDestroyed(), instruction.variablesCreated())) {
                Variable replacement = resolve(renamedVariables, v);
                if (!v.equals(replacement))
                    instruction.replaceVariable(v, replacement);
            }

            for (Variable variable : instruction.variablesDestroyed()) {
                activeVariables.remove(variable);
            }

            for (Variable variable : destroyed) {
                renamedVariables.remove(variable);
            }

            if (site.includes(instruction)) { // If site includes an instruction, it must be a call.
                final List<Instruction> inlinedInstructions = inlineCall((CallInstruction) instruction);
                final var corrector = new ParameterListCorrector(activeVariables);
                inlinedInstructions.forEach(corrector);

                if (corrector.hasModifiedParameterLists()) {
                    combineWith(activeVariables, corrector.additionalVariables, renamedVariables::put);

                    activeVariables.clear(); // Active variables are now renamed.
                    activeVariables.addAll(corrector.additionalVariables);
                }

                result.addAll(inlinedInstructions);
            } else {
                result.add(instruction);
            }

            activeVariables.addAll(instruction.variablesCreated());
        }

        return result;
    }

    private Variable resolve(Map<Variable, Variable> map, Variable v) {
        if (map.containsKey(v)) {
            return resolve(map, map.get(v));
        }
        return v;
    }

    /**
     * Utility class to ensure inlined code contains unique variables.
     */
    private static class UniqueVariableRenamer implements Consumer<Instruction> {
        private final AtomicLong uniqueInlinedVariableId;
        private final Map<Variable, Variable> usedRenames = new HashMap<>();

        public UniqueVariableRenamer(AtomicLong uniqueInlinedVariableId) {
            this.uniqueInlinedVariableId = uniqueInlinedVariableId;
        }

        protected Variable generateReplacement(Variable variable) {
            String newName = String.format("in%x_%s", uniqueInlinedVariableId.incrementAndGet(), variable.getRawName());

            Variable replacement = new Variable(newName);
            replacement.number = variable.number;
            return replacement;
        }

        @Override
        public void accept(Instruction instruction) {
            for (Variable variable : concat(instruction.variablesCreated(), instruction.variablesUsedOrDestroyed())) {
                Variable replacement = usedRenames.computeIfAbsent(variable, this::generateReplacement);

                instruction.replaceVariable(variable, replacement);
            }
        }
    }

    /**
     * Utility class to ensure inlined code contains unique labels.
     * <p>
     * This class creates a new {@link Instruction} for every given {@link ControlInstruction}. To recognize errors
     * in the application of this class, it raises an error, if it is applied to a non {@link ControlInstruction}.
     * <p>
     * If instances of this class is to be used as a function to transform {@link Instruction Instructions} the
     * method {@link UniqueLabelRenamer#asFunction()} should be used instead.
     */
    private static class UniqueLabelRenamer extends DoNothingRSSAVisitor<Instruction> {
        private final AtomicLong uniqueInlinedLabelId;
        private final Map<String, String> usedRenames = new HashMap<>();

        private static <R> R reportIllegalAccess() throws IllegalStateException {
            throw new IllegalStateException("Only instances of " + ControlInstruction.class.getCanonicalName() +
                    " are supported!");
        }

        public UniqueLabelRenamer(AtomicLong uniqueInlinedLabelId) {
            super(UniqueLabelRenamer::reportIllegalAccess); // Report error if instance is not supported.
            this.uniqueInlinedLabelId = uniqueInlinedLabelId;
        }

        /**
         * Turns this visitor into a total function on {@link Instruction} instances that performs the
         * identity function on unsupported {@link Instruction Instructions} instead of raising an error.
         */
        public Function<Instruction, Instruction> asFunction() {
            return instruction -> {
                if (instruction instanceof ControlInstruction) {
                    return instruction.accept(this);
                } else {
                    return instruction;
                }
            };
        }

        // Generates a unique replacement for a label.
        private String generateReplacement(String originalLabel) {
            return String.format("IN%x_%s", uniqueInlinedLabelId.incrementAndGet(), originalLabel);
        }

        // Looks up the unique replacement for a label.
        private String renamedLabel(String originalLabel) {
            return usedRenames.computeIfAbsent(originalLabel, this::generateReplacement);
        }

        // Copies parameter lists from one instruction to another.
        private <C extends ControlInstruction> C withCopiedParameters(C instruction, C source) {
            instruction.addParameters(source.getParameters());
            return instruction;
        }

        @Override
        public Instruction visit(ConditionalEntry instruction) {
            return withCopiedParameters(
                    new ConditionalEntry(
                            renamedLabel(instruction.trueLabel),
                            renamedLabel(instruction.falseLabel),
                            instruction.getCondition()),
                    instruction);
        }

        @Override
        public Instruction visit(ConditionalExit instruction) {
            return withCopiedParameters(
                    new ConditionalExit(
                            renamedLabel(instruction.trueLabel),
                            renamedLabel(instruction.falseLabel),
                            instruction.getCondition()),
                    instruction);
        }

        @Override
        public Instruction visit(UnconditionalEntry instruction) {
            return withCopiedParameters(
                    new UnconditionalEntry(renamedLabel(instruction.label)),
                    instruction);
        }

        @Override
        public Instruction visit(UnconditionalExit instruction) {
            return withCopiedParameters(
                    new UnconditionalExit(renamedLabel(instruction.label)),
                    instruction);
        }
    }

    /**
     * Utility class to add parameters to inlined {@link ControlInstruction ControlInstructions} as they
     * have to pass through live {@link Variable Variables} from the inlining procedure.
     * <p>
     * This class mutates {@link ControlInstruction ControlInstructions}.
     */
    private static class ParameterListCorrector implements Consumer<Instruction> {
        private boolean hasModifiedLists;
        private final int variableCount;
        private final List<Variable> additionalVariables;

        public ParameterListCorrector(Collection<Variable> additionalVariables) {
            this.variableCount = additionalVariables.size();
            this.additionalVariables = new ArrayList<>(additionalVariables);
        }

        @Override
        public void accept(Instruction instruction) {
            if (instruction instanceof ControlInstruction controlInstruction) {
                hasModifiedLists = true;

                if (controlInstruction.isEntryPoint()) {
                    additionalVariables.clear();
                    for (int i = 0; i < variableCount; i++) {
                        additionalVariables.add(Variable.nextTemporaryVariable());
                    }
                }

                for (Variable additionalVariable : additionalVariables) {
                    controlInstruction.addParameter(additionalVariable.copy());
                }
            }
        }

        public boolean hasModifiedParameterLists() {
            return hasModifiedLists;
        }
    }
}