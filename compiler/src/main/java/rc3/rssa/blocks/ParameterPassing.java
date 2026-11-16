package rc3.rssa.blocks;

import rc3.rssa.VariableRenamer;
import rc3.rssa.instances.ArithmeticAssignment;
import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Variable;

import java.util.*;

import static rc3.lib.utils.CollectionUtils.combineWith;

final class ParameterPassing {
    private final List<Instruction> requiredAssignments = new ArrayList<>();
    private final Map<Variable, Atom> requiredRenames = new HashMap<>();

    public ParameterPassing(List<Atom> source, List<Atom> target) {
        combineWith(source, target, (sourceAtom, targetAtom) -> {
            if (targetAtom instanceof Variable targetVariable) {
                requiredRenames.put(targetVariable, sourceAtom);
            } else if (sourceAtom instanceof Variable sourceVariable) {
                requiredRenames.put(sourceVariable, targetAtom);
            } else if (!Objects.equals(sourceAtom, targetAtom)) { // Both are different constants.
                requiredAssignments.add(ArithmeticAssignment.assignAndDestroy(targetAtom, sourceAtom));
            }
        });
    }

    public List<Instruction> getRequiredAssignments() {
        return requiredAssignments;
    }

    public VariableRenamer getRequiredRenames() {
        return VariableRenamer.fromMap(requiredRenames);
    }
}
