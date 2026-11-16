package rc3.rssa.blocks;

import rc3.januscompiler.Direction;
import rc3.rssa.instances.ControlInstruction;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This utility class provides a single static public-facing method,
 * that is used to find groups of {@link ControlInstruction}s in a
 * {@link ControlGraph} that are connected.
 * <p>
 * Consider a {@link ControlGraph} as in the following example:
 * <pre>
 *  begin A
 *  B ->
 *
 *  B()C <-
 *  C()D ->
 *
 *  D <-
 *  end A
 * </pre>
 * When making changes to the parameter list of <code>D</code>, the
 * parameter list of <code>C</code> has to be changed as well to
 * ensure consistency. Because there is a conditional control
 * point, connecting these two. However, <code>C</code> is also
 * connected to <code>B</code>, so changes have to propagate
 * even further, requiring a change of <code>B</code> as well.
 * <p>
 * This class provides a method, which collects all these connected
 * control points into a single {@link Set}. This is called a
 * <i>connected group</i>. The result of this method
 * {@link ConnectedControlPoints#connectedGroups(ControlGraph)} is
 * the {@link Set} of all connected groups in a {@link ControlGraph}.
 */
public final class ConnectedControlPoints {
    private ConnectedControlPoints() throws IllegalAccessError {
        throw new IllegalAccessError();
    }

    /**
     * Returns a {@link Set} of all connected groups in the given {@link ControlGraph}.
     *
     * @implSpec This operation is performed in linear time in respect to the
     * amount of {@link BasicBlock}s of a {@link ControlGraph}. This is equivalent
     * to the amount of {@link ControlInstruction}s, since every {@link BasicBlock}
     * defines exactly 2 of them.
     */
    public static Set<Set<ControlInstruction>> connectedGroups(ControlGraph graph) {
        final Set<Set<ControlInstruction>> groups = new HashSet<>();
        final Set<ControlInstruction> alreadyVisited = new HashSet<>();

        for (ControlInstruction instruction : graph.controlPoints()) {
            if (!alreadyVisited.contains(instruction)) {
                final Set<ControlInstruction> group = new HashSet<>();
                connectedGroup(graph, instruction, group);

                alreadyVisited.addAll(group);
                groups.add(group);
            }
        }

        return groups;
    }

    /**
     * Find the connected group the given {@link ControlInstruction} is part of.
     * <p>
     * All elements of this group are added to the mutable {@link Set} given as
     * an input to this method.
     */
    private static void connectedGroup(ControlGraph graph, ControlInstruction instruction,
                                       Set<ControlInstruction> resultReference) {
        // Only look for connected nodes, if this instruction is not already included in result.
        if (resultReference.add(instruction)) {
            final Stream<String> labels = instruction.associatedLabels().stream();
            if (instruction.isEntryPoint()) {
                labels.flatMap(label -> graph.byLabel(label, Direction.BACKWARD).stream())
                        .map(BasicBlock::exitPoint)
                        .forEach(predecessor -> connectedGroup(graph, predecessor, resultReference));
            } else {
                labels.flatMap(label -> graph.byLabel(label, Direction.FORWARD).stream())
                        .map(BasicBlock::entryPoint)
                        .forEach(successor -> connectedGroup(graph, successor, resultReference));
            }
        }
    }
}
