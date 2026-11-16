package rc3.rssa.control;

import rc3.januscompiler.Direction;
import rc3.lib.DirectionVar;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.BinaryOperand;
import rc3.rssa.instances.ConditionalEntry;
import rc3.rssa.instances.ConditionalExit;
import rc3.rssa.instances.ControlInstruction;

import java.util.*;
import java.util.function.Function;

/**
 * A wrapper class for {@link ControlGraph} that statically analyses
 * {@link ControlInstruction.Conditional#getCondition() the condition
 * of conditional entry and exit points} in order to statically trace
 * possible control flow.
 * The methods {@link ReachableControlGraph#getReachableSuccessors(String)}
 * and {@link ReachableControlGraph#getReachablePredecessors(String)}
 * provide means to access instances in a graph-like manner and only
 * report predecessors or successors that are selectable at runtime.
 * If the <code>boolean</code> value of a condition is statically known,
 * so that it will never be chosen, it won't be included by these methods.
 * <p>
 * The utility method {@link ReachableControlGraph#reachableLabels(Direction)}
 * provides broader access using the specialized predecessor and successor
 * operations to collect all reachable labels into a single {@link Set}.
 *
 * @see BinaryOperand#compareConstant() for the spec of statically evaluating
 * conditions.
 */
public record ReachableControlGraph(ControlGraph graph) {

    /**
     * Returns the {@link Set} of labels that are reachable by
     * executing the {@link ControlGraph} under the given {@link Direction}.
     */
    public Set<String> reachableLabels(Direction direction) {
        final ControlInstruction root =
                direction.choose(graph.getBegin().exitPoint(), graph.getEnd().entryPoint());
        final Function<String, Set<String>> successors =
                direction.choose(this::getReachableSuccessors, this::getReachablePredecessors);

        final Queue<String> queue = new ArrayDeque<>(reachableLabels(root));
        final Set<String> reachable = new HashSet<>();
        while (!queue.isEmpty()) {
            final String label = queue.poll();

            if (reachable.add(label)) {
                queue.addAll(successors.apply(label));
            }
        }

        return reachable;
    }


    /**
     * Returns all labels associated with a {@link ControlInstruction}, excluding those
     * not reachable because of statically known conditions.
     */
    private Set<String> reachableLabels(ControlInstruction instruction) {
        return switch (instruction) {
            case ControlInstruction.Conditional conditional -> {
                final var constantCondition = conditional.getCondition().compareConstant();
                if (constantCondition.isPresent()) {
                    yield Set.of(conditional.getLabel(constantCondition.get()));
                } else {
                    yield Set.of(conditional.trueLabel(), conditional.falseLabel());
                }
            }
            case ControlInstruction.Unconditional unconditional -> Set.of(unconditional.label());
            default -> Collections.emptySet();
        };
    }

    /**
     * Returns the {@link Set} of predecessors or successors of a label, depending
     * on the provided {@link Direction}.
     * <ul>
     *  <li>If the {@link Direction} is {@link Direction#FORWARD},
     *  the {@link BasicBlock} whose <b>entry</b> point includes the given label is fetched
     *  and the reachable labels in its <b>exit</b> point are returned.</li>
     *  <li>If the {@link Direction} is {@link Direction#BACKWARD},
     *  the {@link BasicBlock} whose <b>exit</b> point includes the given label is fetched
     *  and the reachable labels in its <b>entry</b> point are returned.</li>
     * </ul>
     */
    private Set<String> neighbouredLabels(String label, Direction direction) {
        final BasicBlock block = graph.getByLabel(label, direction);
        final ControlInstruction control = direction.chooseAccess(
                BasicBlock::exitPoint, BasicBlock::entryPoint, block);
        return reachableLabels(control);
    }


    /**
     * Returns a {@link Set} of labels that are reachable labels in the
     * {@link BasicBlock#exitPoint()} of the {@link BasicBlock} starting
     * with the given label.
     */
    public Set<String> getReachableSuccessors(String label) {
        return neighbouredLabels(label, Direction.FORWARD);
    }


    /**
     * Returns a {@link Set} of labels that are reachable labels in the
     * {@link BasicBlock#entryPoint()} of the {@link BasicBlock} ending
     * with the given label.
     */
    public Set<String> getReachablePredecessors(String label) {
        return neighbouredLabels(label, Direction.BACKWARD);
    }


    public String toString() {
        final DirectionVar<Set<String>> reachable = DirectionVar.instantiate(this::reachableLabels);
        final StringBuilder sb = new StringBuilder();
        sb.append("digraph { ");

        for (BasicBlock node : graph.getNodes()) {
            final boolean topIsConditional = node.entryPoint() instanceof ConditionalEntry;
            final boolean botIsConditional = node.exitPoint() instanceof ConditionalExit;
            final String shape;
            if (topIsConditional && botIsConditional) shape = "diamond";
            else if (topIsConditional) shape = "house";
            else if (botIsConditional) shape = "invhouse";
            else shape = "rectangle";

            final boolean isBeginOrEnd = node == graph.getBegin() || node == graph.getEnd();

            sb.append(node.graphId())
                    .append("[label=\"\"") // No text, just show the shape.
                    .append(",shape=").append(shape)
                    .append(",peripheries=").append(isBeginOrEnd ? 2 : 1)
                    .append("]; ");
        }

        for (String label : graph.getLabels()) {
            final DirectionVar<Boolean> isReachable = reachable.map(set -> set.contains(label));
            final String color;
            if (isReachable.get(Direction.FORWARD) && isReachable.get(Direction.BACKWARD)) color = "green";
            else if (isReachable.get(Direction.FORWARD)) color = "blue";
            else if (isReachable.get(Direction.BACKWARD)) color = "yellow";
            else color = "black";

            sb.append(graph.getByLabel(label, Direction.BACKWARD).graphId())
                    .append(" -> ")
                    .append(graph.getByLabel(label, Direction.FORWARD).graphId())
                    .append("[label=\"").append(label).append("\"")
                    .append("color=").append(color)
                    .append("];");
        }
        sb.append(" }");
        return sb.toString();
    }
}
