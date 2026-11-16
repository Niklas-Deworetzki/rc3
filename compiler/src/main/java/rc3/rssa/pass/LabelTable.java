package rc3.rssa.pass;

import rc3.januscompiler.Direction;
import rc3.lib.DirectionVar;
import rc3.rssa.instances.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LabelTable {
    private final Program program;
    private final List<Instruction> instructions;

    public LabelTable(Program program) {
        this.program = program;
        this.instructions = program.asListOfInstructions();
    }

    private DirectionVar<Map<String, Integer>> getLabelPositions() {
        final DirectionVar<Map<String, Integer>> labelPositions = DirectionVar.of(HashMap::new);

        for (int position = 0; position < instructions.size(); position++) {
            if (instructions.get(position) instanceof ControlInstruction control) {
                // Find the map where this control instruction is a target.
                final Map<String, Integer> positionMap = labelPositions.get(control.isEntryPoint ?
                        Direction.FORWARD : Direction.BACKWARD);

                for (String associatedLabel : control.associatedLabels()) {
                    positionMap.put(associatedLabel, position);
                }
            }
        }
        return labelPositions;
    }

    /**
     * Build the <i>Label Table</i> for a {@link Program}.
     * <p>
     * This table holds a {@link LabelEntry} for every defined label in the {@link Program}.
     * It can be used to perform jumps to a label (both in forward and backward direction),
     * as well as check the amount of parameters required for a jump, whether a label
     * is a procedure and how much stack space should be allocated for a procedures stack frame.
     */
    public Map<String, LabelEntry> build() {
        final Map<String, LabelEntry> labelTable = new HashMap<>();
        final var labelPositions = getLabelPositions();

        final Set<String> allLabels = labelPositions.get(Direction.FORWARD).keySet();
        for (String label : allLabels) {
            int fwPosition = labelPositions.get(Direction.FORWARD).get(label);
            int bwPosition = labelPositions.get(Direction.BACKWARD).get(label);
            boolean isProcedure = program.procedures().containsKey(label);
            int frameSize = -1;
            if (isProcedure) {
                frameSize = program.procedures().get(label).frameSize;
            }

            labelTable.put(label, new LabelEntry(
                    isProcedure, frameSize,
                    fwPosition, bwPosition
            ));
        }

        return labelTable;
    }
}
