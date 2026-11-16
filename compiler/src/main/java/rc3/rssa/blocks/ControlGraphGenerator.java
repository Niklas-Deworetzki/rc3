package rc3.rssa.blocks;

import rc3.rssa.instances.ControlInstruction;
import rc3.rssa.instances.Instruction;

import java.util.*;

/**
 * Given a List of {@link Instruction Instructions}, this class generates a {@link ControlGraph} representing
 * the control flow within the instructions.
 * <p>
 * Generation of the {@link ControlGraph} is split into multiple steps:
 * <ol>
 *  <li>Organize instructions into {@link BasicBlock BasicBlocks}</li>
 *  <li>Insert edges between those blocks</li>
 * </ol>
 */
public final class ControlGraphGenerator {

    private final ControlGraph graph;
    private final List<Instruction> code;

    private int blockNumber = 0;
    private boolean graphGenerated = false;

    public ControlGraphGenerator(String procedureName, List<Instruction> code) {
        this.graph = new ControlGraph(procedureName);
        this.code = code;
    }

    public static Map<String, ControlGraph> fromProcedures(Map<String, List<Instruction>> translatedProcedures) {
        final Map<String, ControlGraph> result = new HashMap<>(translatedProcedures.size());

        for (var entry : translatedProcedures.entrySet()) {
            result.put(entry.getKey(), translateProcedure(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    public static ControlGraph translateProcedure(String procedureName, List<Instruction> translatedProcedure) {
        var generator = new ControlGraphGenerator(procedureName, translatedProcedure);
        return generator.getGraph();
    }

    /**
     * Generates and returns the {@link ControlGraph} for the given input code.
     */
    public synchronized ControlGraph getGraph() {
        if (!graphGenerated) {
            generateGraph();
            graphGenerated = true;
        }
        return graph;
    }

    /**
     * Organizes the instructions passed to the generator into a graph representing the possible control flow.
     */
    private void generateGraph() {
        final Set<BasicBlock> allBlocks = new HashSet<>();

        BasicBlock activeBlock = new BasicBlock(blockNumber++);
        allBlocks.add(activeBlock);

        Iterator<Instruction> instructions = code.iterator();
        activeBlock.addInstruction(instructions.next());

        while (instructions.hasNext()) {
            final Instruction instruction = instructions.next();

            if (instruction instanceof ControlInstruction) {
                if (((ControlInstruction) instruction).isEntryPoint()) {
                    allBlocks.add(activeBlock);
                    activeBlock = new BasicBlock(blockNumber++);
                }
            }
            activeBlock.addInstruction(instruction);
        }

        allBlocks.add(activeBlock);
        graph.updateNodes(allBlocks);
    }
}
