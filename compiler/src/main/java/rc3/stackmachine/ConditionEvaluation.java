package rc3.stackmachine;

import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.ControlInstruction;

import java.util.*;

/**
 * This utility class provides a single, public facing method:
 * {@link ConditionEvaluation#findConsentForConditions(ControlGraph)}
 * <p>
 * The purpose of this class is to find consent for the evaluation of conditions
 * when entering or exiting a {@link BasicBlock}. In RSSA it is possible to exit
 * a block using label <var>A</var> with a condition evaluating to <code>true</code>,
 * but entering another block using the label <var>A</var> with a condition evaluating
 * to <code>false</code>.
 * <p>
 * The stack machine code translation scheme does not support this case directly,
 * since <code>brt</code> and <code>brf</code> instructions are used to perform
 * conditional jumps and the condition stays unevaluated on the stack.
 * <p>
 * As a solution to this difference, we have to find consent between different conditions,
 * such that a label is either always associated with a <code>true</code> condition or
 * always associated with a <code>false</code> condition. These decisions are encoded
 * using the {@link TargetValue} enumeration, which also includes a {@link TargetValue#NONE}
 * constant representing unconditional jumps. This value only appears, if an unconditional
 * jump goes to an unconditional target, as in this case no condition has to be placed
 * on the stack or has to be verified. If unconditional and conditional jumps interact,
 * the unconditional jump can simply place a corresponding truth value onto the stack.
 * <p>
 * This implementation constructs a graph where labels are nodes and an edge exists
 * between labels that occur within the same control point. Connected groups within
 * this graph are either rings or chains (lists) that can easily be <i>colored</i>
 * <code>true</code> or <code>false</code>. To ensure a valid RSSA and stack machine
 * program, the only restriction is that two adjacent nodes cannot have the same color.
 * As an optimization unconnected nodes can be colored using {@link TargetValue#NONE},
 * since they do not appear in any conditional control point.
 */
final class ConditionEvaluation {
    private final Map<String, Set<String>> connectedEdges = new HashMap<>();
    private final Map<String, TargetValue> foundConsent = new HashMap<>();

    private ConditionEvaluation(ControlGraph graph) {
        // Construct a graph where every label is connected to all other labels that occur in a control point.
        for (ControlInstruction instruction : graph.controlPoints()) {
            Set<String> labels = instruction.associatedLabels();

            for (String label : labels) {
                Set<String> neighbours = connectedEdges.computeIfAbsent(label, ignored -> new HashSet<>());
                neighbours.addAll(labels); // This operation is simple but every label now contains an edge to itself.
            }
        }

        // Delete edges from each node to itself.
        for (var entry : connectedEdges.entrySet()) {
            entry.getValue().remove(entry.getKey());
        }
    }

    public static Map<String, TargetValue> findConsentForConditions(ControlGraph graph) {
        return new ConditionEvaluation(graph).solve();
    }

    private void walkGraphAsLine(String currentEdge, TargetValue color) {
        if (!foundConsent.containsKey(currentEdge)) {
            foundConsent.put(currentEdge, color);

            for (String neighbouringEdges : connectedEdges.get(currentEdge)) {
                walkGraphAsLine(neighbouringEdges, color.negate());
            }
        }
    }

    /**
     * Iterates every group in the graph, assigning the nodes in those groups
     * alternating colors. If a group only contains a single element, the
     * color {@link TargetValue#NONE} is assigned.
     */
    private Map<String, TargetValue> solve() {
        for (var entry : connectedEdges.entrySet()) {
            if (foundConsent.containsKey(entry.getKey())) continue;

            if (entry.getValue().isEmpty()) { // A single unconnected node.
                foundConsent.put(entry.getKey(), TargetValue.NONE);
            } else {
                walkGraphAsLine(entry.getKey(), TargetValue.TRUE);
            }
        }
        return foundConsent;
    }

    /**
     * This enum holds the different truth values that are used to assert a control path.
     * <p>
     * In practical terms, the value represented by this enum is the value via which a
     * label is entered/exited in the stackmachine code.
     */
    enum TargetValue {
        /**
         * Control path is asserted using <code>true</code>.
         */
        TRUE,
        /**
         * Control path is asserted using <code>false</code>.
         */
        FALSE,
        /**
         * No assertion is necessary for control path.
         */
        NONE;

        /**
         * Returns the inverse {@link TargetValue} used to generate alternating values.
         */
        TargetValue negate() {
            return switch (this) {
                case TRUE -> FALSE;
                case FALSE -> TRUE;
                case NONE -> NONE;
            };
        }
    }
}
