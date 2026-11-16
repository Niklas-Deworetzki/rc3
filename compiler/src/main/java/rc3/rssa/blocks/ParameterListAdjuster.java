package rc3.rssa.blocks;

import rc3.rssa.instances.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParameterListAdjuster implements Runnable {

    public static void adjustParameterLists(ControlGraph graph) {
        new ParameterListAdjuster(graph).run();
    }

    public static void adjustParameterLists(Program program) {
        program.procedures().values().forEach(ParameterListAdjuster::adjustParameterLists);
    }

    private final ControlGraph graph;

    public ParameterListAdjuster(ControlGraph graph) {
        this.graph = graph;
    }


    @Override
    public void run() {
        boolean parametersHaveChanged;
        do {
            parametersHaveChanged = adjustParameterLists();
        } while (parametersHaveChanged);
    }

    private boolean adjustParameterLists() {
        boolean changed = false;
        for (BasicBlock block : graph) {
            changed |= adjustOwnParameterList(block);
            changed |= adjustPassedParameterList(block);
        }
        return changed;
    }

    /**
     * Returns a {@link Set} of all {@link Variable Variables} that used within a {@link BasicBlock}
     * but are not defined in the same {@link BasicBlock} before being used.
     */
    private Set<Variable> findRequiredParameters(BasicBlock block) {
        Set<Variable> defined = new HashSet<>();
        Set<Variable> result = new HashSet<>();


        for (Instruction instruction : block.getInstructions()) {
            if (instruction instanceof ControlInstruction) {
                // L1(x)L2 -> x == 0  requires x to be created, before it is used.
                // x := x + 1         requires x to be used, before it is created.
                // So ControlInstructions are "create before use" whereas every instruction is use before create.
                defined.addAll(instruction.variablesCreated());
            }

            for (Variable used : instruction.variablesUsedOrDestroyed()) {
                if (!defined.contains(used)) {
                    result.add(used);
                }
            }

            defined.addAll(instruction.variablesCreated());
        }

        return result;
    }

    /**
     * Add required parameters to parameter list of the given {@link BasicBlock}.
     */
    private boolean adjustOwnParameterList(BasicBlock block) {
        ControlInstruction entryPoint = block.entryPoint();
        Set<Variable> missingParameters = findRequiredParameters(block);

        missingParameters.removeIf(entryPoint.getParameters()::contains);
        entryPoint.addParameters(missingParameters);
        return !missingParameters.isEmpty();
    }

    /**
     * Iterates all successors of a given {@link BasicBlock} and notes which parameters they require.
     * If all required parameters are collected, the parameter lists of all successors are modified to
     * contain all the required parameters. The outgoing parameter list of the given {@link BasicBlock}
     * has to be modified as well.
     *
     * @return <code>true</code> if a change was made.
     */
    private boolean adjustPassedParameterList(BasicBlock block) {
        ControlInstruction exitPoint = block.exitPoint();
        List<Atom> childrenParameterList = exitPoint.getParameters();

        boolean changed = false;
        for (BasicBlock successor : graph.getSuccessors(block)) {
            for (Atom successorInputParameter : successor.inputParameters()) {
                if (!childrenParameterList.contains(successorInputParameter)) {
                    childrenParameterList.add(successorInputParameter);
                    changed = true;
                }
            }
        }
        for (BasicBlock successor : graph.getSuccessors(block)) {
            ControlInstruction successorEntry = successor.entryPoint();

            successorEntry.getParameters().clear();
            successorEntry.addParameters(childrenParameterList);
        }

        return changed;
    }
}
