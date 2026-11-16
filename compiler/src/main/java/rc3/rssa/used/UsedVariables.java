package rc3.rssa.used;

import rc3.januscompiler.Direction;
import rc3.lib.dataflow.DataflowProblem;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ConnectedControlPoints;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static rc3.lib.utils.CollectionUtils.*;
import static rc3.lib.utils.SetOperations.intersection;

/**
 * This analysis pass is used to detect which {@link Variable}s within
 * a {@link ControlGraph} are <i>used</i>. A {@link Variable} is
 * <i>used</i>, if it is required for the computations performed by the
 * {@link ControlGraph}.
 * <p>
 * Unused {@link Variable}s, therefore, are those that aren't required
 * for a computation. These {@link Variable}s can be removed as well
 * as {@link Instruction}s working with them, without changing the
 * computation performed.
 */
public final class UsedVariables {

    /**
     * The result of analysis contains a {@link Set} of all used {@link Variable}
     * instances as well as a mapping, marking which parameters of a parameter
     * list are in use.
     */
    public record Result(Set<Variable> usedVariables,
                         Map<String, List<Boolean>> usedInParameterList) {

        public List<Boolean> getUsedInParameterList(ControlInstruction instruction) {
            return usedInParameterList.get(anyElement(instruction.associatedLabels()));
        }
    }

    // Combine the "use" property of two parameter lists.
    private static List<Boolean> combineParameterLists(List<Boolean> as, List<Boolean> bs) {
        return zipWith(as, bs, Boolean::logicalOr).toList();
    }

    private static List<Boolean> usedInEndInstruction(EndInstruction endInstruction) {
        return endInstruction.getParameters().stream()
                .map(Variable.class::isInstance)
                .toList();
    }

    private static Set<Variable> findAllVariablesInBlockAfterAnalysis(BasicBlock block,
                                                                      Map<String, List<Boolean>> usedInParameterList) {
        final ControlInstruction exitPoint = block.exitPoint();
        final List<Boolean> usedInBlock = (exitPoint instanceof EndInstruction endInstruction)
                ? usedInEndInstruction(endInstruction)
                : usedInParameterList.get(anyElement(exitPoint.associatedLabels()));

        return variablesUsedInBlock(block, usedInBlock);
    }

    /**
     * The {@link Result} of this analysis returns a {@link Set} of all {@link Variable}s
     * that are used within the given {@link ControlGraph procedure}. Additionally, for
     * every label present in the program, information is provided to decide, whether
     * a parameter for this label is used.
     * <p>
     * This method will solve the {@link UsedVariableAnalysis} dataflow problem twice,
     * once in each direction, and combine the results of both analysis.
     */
    public static Result usedVariableAnalysis(ControlGraph fwProcedure) {
        final ControlGraph bwProcedure = fwProcedure.reversed();

        // Since we are modifying parameter lists, we have to find all connected labels.
        final Set<Set<String>> connectedGroupLabels = ConnectedControlPoints.connectedGroups(fwProcedure)
                .stream()
                .map(group -> group.stream() // Convert each group of ControlInstruction to a group of Labels.
                        .map(ControlInstruction::associatedLabels).flatMap(Set::stream)
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        // Remove the begin and exit label from the set, since they are excluded from analysis.
        connectedGroupLabels.removeIf(group -> group.contains(fwProcedure.getProcedureName()));

        // Solve problem in both directions.
        final var fwResult = new UsedVariableAnalysis(fwProcedure).solve();
        final var bwResult = new UsedVariableAnalysis(bwProcedure).solve();

        final Map<String, List<Boolean>> usedInParameterList = new HashMap<>();
        for (Set<String> group : connectedGroupLabels) {
            // Combine the IN and OUT information about every parameter list in every direction for every list in group.
            // This makes the result direction agnostic, so we only have to know the label hereafter.
            final List<Boolean> usedParametersInGroup = group.stream().flatMap(label -> {
                        final var fwIn = fwProcedure.byLabel(label, Direction.FORWARD).orElseThrow();
                        final var fwOut = fwProcedure.byLabel(label, Direction.BACKWARD).orElseThrow();
                        final var bwIn = bwProcedure.byLabel(label, Direction.FORWARD).orElseThrow();
                        final var bwOut = bwProcedure.byLabel(label, Direction.BACKWARD).orElseThrow();

                        return Stream.of(
                                fwResult.getIn(fwIn), fwResult.getOut(fwOut),
                                bwResult.getIn(bwIn), bwResult.getOut(bwOut));
                    })
                    .reduce(UsedVariables::combineParameterLists).orElseThrow();

            for (String label : group) {
                usedInParameterList.put(label, usedParametersInGroup);
            }
        }

        final Set<Variable> usedVariables = new HashSet<>();
        // Perform the Block-level analysis again with the combined information about each parameter list.
        for (BasicBlock block : fwProcedure) {
            usedVariables.addAll(findAllVariablesInBlockAfterAnalysis(block, usedInParameterList));
        }
        for (BasicBlock block : bwProcedure) {
            usedVariables.addAll(findAllVariablesInBlockAfterAnalysis(block, usedInParameterList));
        }
        return new Result(usedVariables, usedInParameterList);
    }


    /**
     * {@link DataflowProblem} to compute which variables are used within a program, by tracing
     * uses from the exit point through the different {@link BasicBlock blocks} following the
     * parameter lists.
     * <p>
     * This problem is solved bottom up by marking all {@link Variable}s in the parameter list
     * of a procedures {@link EndInstruction} as used.
     */
    private static class UsedVariableAnalysis extends DataflowProblem<BasicBlock, List<Boolean>> {
        public UsedVariableAnalysis(ControlGraph procedure) {
            super(procedure.getNodes(), procedure::getPredecessors, procedure::getSuccessors,
                    Direction.BACKWARD);
        }

        @Override
        public List<Boolean> initialInValue(BasicBlock node) {
            // We don't know if any variables are used here.
            return fillList(false, node.entryPoint().getParameters().size());
        }

        @Override
        public List<Boolean> initialOutValue(BasicBlock node) {
            final ControlInstruction exitPoint = node.exitPoint();
            if (exitPoint instanceof EndInstruction endInstruction) {
                // An end instruction uses all Variables in its parameter list.
                return usedInEndInstruction(endInstruction);
            }

            // All other instructions are unknown as well.
            return fillList(false, exitPoint.getParameters().size());
        }

        @Override
        public List<Boolean> combine(List<Boolean> a, List<Boolean> b) {
            return combineParameterLists(a, b);
        }

        @Override
        public List<Boolean> transfer(List<Boolean> usedInParameterList, BasicBlock block) {
            return toParameterList(block.entryPoint().getParameters(),
                    variablesUsedInBlock(block, usedInParameterList));
        }
    }

    /**
     * Given a {@link Set} of {@link Variable Variables} that are used, this method returns a {@link List}
     * of boolean values, indicating which parameters of a parameter list are used.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    private static List<Boolean> toParameterList(List<Atom> parameterList, Set<Variable> usedVariables) {
        return parameterList.stream()
                .map(usedVariables::contains)
                .toList();
    }

    /**
     * Given a {@link List} of boolean values, this method returns a {@link Set} of {@link Variable Variables},
     * including every {@link Variable} in a parameter list, for which the boolean list indicates, that
     * it is used.
     */
    private static Set<Variable> fromParameterList(List<Atom> parameterList, List<Boolean> usedParameters) {
        Set<Variable> usedVariables = new HashSet<>();
        combineWith(parameterList, usedParameters, (parameter, isUsed) -> {
            if (isUsed && parameter instanceof Variable)
                usedVariables.add((Variable) parameter);
        });
        return usedVariables;
    }

    /**
     * Returns the {@link Set} of all {@link Variable Variables} that are used within the given {@link BasicBlock},
     * assuming the variables in the {@link BasicBlock#exitPoint() exit points} parameter list are used
     * as defined by the given list of booleans.
     */
    private static Set<Variable> variablesUsedInBlock(BasicBlock block, List<Boolean> initiallyUsed) {
        final Set<Variable> usedVariables = new HashSet<>(fromParameterList(block.exitPoint().getParameters(), initiallyUsed));

        for (Instruction instruction : reverse(block.getInstructions())) {
            usedVariables.addAll(use(instruction, usedVariables));
        }

        return usedVariables;
    }

    private static boolean hasSideEffects(Instruction instruction) {
        return instruction instanceof CallInstruction
                || instruction instanceof MemoryAssignment
                || instruction instanceof MemoryInterchangeInstruction
                || instruction instanceof MemorySwapInstruction;
    }

    private static Set<Variable> use(Instruction instruction, Set<Variable> used) {
        if (hasSideEffects(instruction) || // Instructions having a side-effect cannot be eliminated
                !intersection(used, Set.copyOf(instruction.variablesCreated())).isEmpty()) { // Construct a chain, if the destroyed variable is already used.
            return Set.copyOf(instruction.variablesUsedOrDestroyed());
        } else if (instruction instanceof ArithmeticAssignment) {
            return emptySet();
        }
        return Set.copyOf(instruction.variablesUsed());
    }


    public static void dumpLiveVariables(Program rssaProgram) {
        for (Map.Entry<String, ControlGraph> procedure : rssaProgram.procedures().entrySet()) {
            System.out.println("Procedure " + procedure.getKey() + " uses: ");
            System.out.println(formatSet(usedVariableAnalysis(procedure.getValue()).usedVariables()));
        }
    }

    private static String formatSet(Set<Variable> set) {
        return set.stream().map(Variable::getName).collect(Collectors.joining(", ", "{", "}"));
    }
}
