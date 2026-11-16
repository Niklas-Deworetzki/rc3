package rc3.rssa.dce;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;
import rc3.rssa.used.UsedVariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static rc3.lib.utils.SetOperations.concat;

public class DeadCodeElimination extends Program.ProcedureOptimization {

    public DeadCodeElimination(Compiler compiler) {
        super(AvailableOptimization.DEAD_CODE_ELIMINATION, compiler);
    }

    @Override
    public ControlGraph apply(String name, ControlGraph procedure) {
        debug("\n\n======= DCE on " + name);
        debug("\n Code before DCE:");
        debug(procedure::graphString);

        debug("\nDCE:");
        eliminateCode(procedure);
        debug(" Code after DCE:");
        debug(procedure::graphString);

        return procedure;
    }

    private void eliminateCode(ControlGraph procedure) {
        final UsedVariables.Result result = UsedVariables.usedVariableAnalysis(procedure);
        final var used = result.usedVariables();
        debug("USED of %s:%n%s%n", procedure.getProcedureName(), used);

        for (BasicBlock block : procedure) {
            List<Instruction> optimizedCode = new ArrayList<>();

            for (Instruction instr : block) {
                if (instr instanceof ControlInstruction controlInstruction &&
                        !(instr instanceof BeginInstruction) && !(instr instanceof EndInstruction)) {
                    final List<Atom> parameters = controlInstruction.getParameters();
                    final List<Boolean> isUsed = result.getUsedInParameterList(controlInstruction);

                    final List<Atom> removedParameters = new ArrayList<>(isUsed.size());
                    for (int index = isUsed.size() - 1; index >= 0; index--) {
                        if (!isUsed.get(index)) {
                            removedParameters.add(parameters.remove(index));
                        }
                    }

                    if (!removedParameters.isEmpty()) {
                        debug("REMOVED parameters %s in instruction %s", removedParameters, instr);
                    }
                    optimizedCode.add(instr);

                } else {
                    if (isUsed(instr, used)) {
                        // TODO reformat instruction to a single assignment
                        optimizedCode.add(instr);
                    } else {
                        debug("REMOVED instruction %s", instr);
                    }
                }
            }
            block.setInstructions(optimizedCode);
        }
        debug("\n");
    }

    private boolean isUsed(Instruction instr, Set<Variable> used) {
        for (Variable variable : concat(instr.variablesUsedOrDestroyed(), instr.variablesCreated())) {
            /* An instruction may be deleted iff it uses a variable that is not in USED.
               This works only because of the implementation of the USE-analysis. */
            if (!used.contains(variable)) return false;
        }
        return true;
    }
}
