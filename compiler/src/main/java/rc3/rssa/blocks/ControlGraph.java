package rc3.rssa.blocks;

import rc3.januscompiler.Direction;
import rc3.lib.utils.GraphViz;
import rc3.rssa.instances.BeginInstruction;
import rc3.rssa.instances.ControlInstruction;
import rc3.rssa.instances.EndInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.pass.StackAllocation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static rc3.lib.utils.CollectionUtils.subList;

/**
 * A Graph data structure holding all {@link BasicBlock} nodes of a procedure.
 */
public final class ControlGraph implements GraphViz, Iterable<BasicBlock> {
    private final String procedure;

    // Store a collection of all blocks as well as adjacency lists for every block.
    private final Collection<BasicBlock> nodes = new ArrayList<>();
    private final Map<String, BasicBlock> fwBlocks = new HashMap<>(),
            bwBlocks = new HashMap<>();

    private BasicBlock beginBlock, endBlock;
    /**
     * The total size required to allocate a stack frame for the procedure represented
     * by this Graph.
     * <p>
     * This field is set by {@link StackAllocation#allocateStackFrame(ControlGraph)}.
     */
    public int frameSize = StackAllocation.NOT_ALLOCATED;

    public ControlGraph(String procedure) {
        this.procedure = procedure;
    }

    /**
     * Returns the name of the procedure represented by this Graph.
     */
    public String getProcedureName() {
        return procedure;
    }

    public Collection<BasicBlock> getNodes() {
        return nodes;
    }

    public BasicBlock getBegin() {
        return beginBlock;
    }

    public BeginInstruction getBeginInstruction() {
        return (BeginInstruction) getBegin().entryPoint();
    }

    public BasicBlock getEnd() {
        return endBlock;
    }

    public EndInstruction getEndInstruction() {
        return (EndInstruction) getEnd().exitPoint();
    }

    /**
     * Sets the contents of this {@link ControlGraph} to the given {@link Collection} of
     * {@link BasicBlock}s.
     * <p>
     * All internal fields are automatically updated.
     */
    public void updateNodes(Collection<BasicBlock> nodes) {
        this.nodes.clear();

        for (BasicBlock node : nodes) {
            if (!this.nodes.contains(node)) {
                this.nodes.add(node);

                if (node.entryPoint() instanceof BeginInstruction) {
                    this.beginBlock = node;
                }
                if (node.exitPoint() instanceof EndInstruction) {
                    this.endBlock = node;
                }
            }
        }

        updateEdges();
    }

    private void updateEdges() {
        fwBlocks.clear();
        bwBlocks.clear();

        for (BasicBlock node : nodes) {
            if (!(node.entryPoint() instanceof BeginInstruction)) {
                for (String label : node.entryLabels()) {
                    fwBlocks.put(label, node);
                }
            }
            if (!(node.exitPoint() instanceof EndInstruction)) {
                for (String label : node.exitLabels()) {
                    bwBlocks.put(label, node);
                }
            }
        }
    }

    /**
     * Returns the {@link BasicBlock} with the given label (if present).
     * <p>
     * Depending on the given {@link Direction}, this method will return a {@link BasicBlock}
     * that can be entered or exited through a given label.
     * <ul>
     *  <li>If the {@link Direction} is {@link Direction#FORWARD} a matching block
     *  can be entered through the given label.</li>
     *  <li>If the {@link Direction} is {@link Direction#BACKWARD} a matching block
     *  can be exited through the given label.</li>
     * </ul>
     * If no such {@link BasicBlock} exists, {@link Optional#empty()} is returned.
     */
    public Optional<BasicBlock> byLabel(String label, Direction direction) {
        return Optional.ofNullable(direction.choose(fwBlocks, bwBlocks).get(label));
    }

    /**
     * Returns the {@link BasicBlock} with the given label (if present).
     * <p>
     * Depending on the given {@link Direction}, this method will return a {@link BasicBlock}
     * that can be entered or exited through a given label.
     * <ul>
     *  <li>If the {@link Direction} is {@link Direction#FORWARD} a matching block
     *  can be entered through the given label.</li>
     *  <li>If the {@link Direction} is {@link Direction#BACKWARD} a matching block
     *  can be exited through the given label.</li>
     * </ul>
     *
     * @throws NoSuchElementException if no matching {@link BasicBlock} exists.
     */
    public BasicBlock getByLabel(String label, Direction direction) throws NoSuchElementException {
        final BasicBlock block = direction.choose(fwBlocks, bwBlocks).get(label);
        if (block == null) throw new NoSuchElementException();
        return block;
    }

    public Set<String> getLabels() {
        return fwBlocks.keySet();
    }

    public Iterable<ControlInstruction> controlPoints() {
        return this::iterateControlPoints;
    }

    public Iterator<ControlInstruction> iterateControlPoints() {
        return new ControlInstructionIterator(this);
    }

    public Set<BasicBlock> getSuccessors(BasicBlock block) {
        Set<BasicBlock> result = new HashSet<>();
        for (String label : block.exitLabels()) {
            byLabel(label, Direction.FORWARD).ifPresent(result::add);
        }
        return result;
    }

    public Set<BasicBlock> getPredecessors(BasicBlock block) {
        Set<BasicBlock> result = new HashSet<>();
        for (String label : block.entryLabels()) {
            byLabel(label, Direction.BACKWARD).ifPresent(result::add);
        }
        return result;
    }


    /**
     * Returns a new {@link ControlGraph} in which every {@link BasicBlock} is inverted
     * using {@link BasicBlock#reversed()}.
     */
    public ControlGraph reversed() {
        return updated(BasicBlock::reversed);
    }

    /**
     * Returns a new {@link ControlGraph} in which every {@link BasicBlock} has
     * been updated using the given {@link Function}.
     */
    public ControlGraph updated(Function<BasicBlock, BasicBlock> updateFunction) {
        final ControlGraph result = new ControlGraph(procedure);
        result.updateNodes(nodes.stream().map(updateFunction).collect(Collectors.toList()));
        return result;
    }

    public BasicBlock join(BasicBlock predecessor, BasicBlock successor) {
        final ControlInstruction exitPoint = predecessor.exitPoint();
        final ControlInstruction entryPoint = successor.entryPoint();

        final var parameterPassing = new ParameterPassing(exitPoint.getParameters(), entryPoint.getParameters());
        final var renamer = parameterPassing.getRequiredRenames();

        final List<Instruction> joinedInstructions = new ArrayList<>();
        // Add all instructions except exit point.
        for (Instruction instruction : subList(predecessor.getInstructions(), 0, 1)) {
            renamer.accept(instruction);
            joinedInstructions.add(instruction);
        }
        // Add instructions to pass parameters.
        joinedInstructions.addAll(parameterPassing.getRequiredAssignments());
        // Add all instructions except entry point.
        for (Instruction instruction : subList(successor.getInstructions(), 1, 0)) {
            renamer.accept(instruction);
            joinedInstructions.add(instruction);
        }

        final BasicBlock joinedBlock = new BasicBlock(predecessor.number);
        joinedBlock.setInstructions(joinedInstructions);

        // Collection of nodes in Graph after joining.
        final List<BasicBlock> updatedNodes = new ArrayList<>();
        updatedNodes.add(joinedBlock);
        for (BasicBlock node : this.nodes) {
            if (node != predecessor && node != successor) {
                updatedNodes.add(node);
            }
        }
        this.updateNodes(updatedNodes);
        return joinedBlock;
    }

    @Override
    public Iterator<BasicBlock> iterator() {
        return nodes.iterator();
    }

    public int codeSize() {
        int size = 0;
        for (BasicBlock node : nodes) {
            size += node.getInstructions().size();
        }
        return size;
    }

    public List<Instruction> toCode() {
        List<Instruction> result = new ArrayList<>();

        // Ensure begin is the first instruction of a procedure.
        result.addAll(getBegin().getInstructions());

        // Iterate all blocks that are part of the procedures body.
        for (BasicBlock node : nodes) {
            if (node != getBegin() && node != getEnd()) {
                result.addAll(node.getInstructions());
            }
        }

        // Do not include begin twice, if begin is also end.
        if (getEnd() != getBegin()) {
            result.addAll(getEnd().getInstructions());
        }
        return result;
    }

    @Override
    public String graphString() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(graphId()).append('{');
        sb.append("node[shape=box,fontname=courier];");

        for (BasicBlock block : nodes) {
            sb.append(block.graphString());
            if (block.equals(beginBlock) || block.equals(endBlock)) {
                sb.append("[peripheries=2]"); // Visually highlight root.
            }
            sb.append(';');
        }
        for (BasicBlock block : nodes) {
            for (BasicBlock successor : getSuccessors(block)) {
                sb.append(block.graphId())
                        .append(" -> ")
                        .append(successor.graphId())
                        .append(';');
            }
            for (BasicBlock predecessor : getPredecessors(block)) {
                sb.append(block.graphId())
                        .append(" -> ")
                        .append(predecessor.graphId())
                        .append("[style=dashed];");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String graphId() {
        return procedure;
    }

    /**
     * This {@link Iterator} uses an {@link Iterator} over all {@link BasicBlock} in the background.
     * Whenever {@link Iterator#next()} is called, one of two cases may apply:
     * <ul>
     *  <li><i>No {@link ControlInstruction} is known.</i> In this case, the next {@link BasicBlock}
     *  is fetched from the backing {@link Iterator}, its exit point is buffered and its entry point
     *  is immediately returned.</li>
     *  <li><i>An {@link ControlInstruction} has been buffered from a previous call</i>. In this case,
     *  it is possible to simply return this buffered {@link ControlInstruction} and invalidate the
     *  buffer.</li>
     * </ul>
     */
    private static final class ControlInstructionIterator implements Iterator<ControlInstruction> {
        private final Iterator<BasicBlock> blockIterator;
        private ControlInstruction bufferedInstruction = null;

        public ControlInstructionIterator(ControlGraph graph) {
            this.blockIterator = graph.iterator();
        }

        @Override
        public boolean hasNext() {
            return bufferedInstruction != null || blockIterator.hasNext();
        }

        @Override
        public ControlInstruction next() {
            if (bufferedInstruction == null) {
                final BasicBlock block = blockIterator.next();
                bufferedInstruction = block.exitPoint();
                return block.entryPoint();
            } else {
                final ControlInstruction result = bufferedInstruction;
                bufferedInstruction = null;
                return result;
            }
        }
    }
}
