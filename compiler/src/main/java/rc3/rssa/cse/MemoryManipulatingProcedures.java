package rc3.rssa.cse;

import rc3.januscompiler.Direction;
import rc3.lib.dataflow.DataflowProblem;
import rc3.lib.dataflow.Lattice;
import rc3.rssa.blocks.CallDependencyGraph;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.DoNothingRSSAVisitor;
import rc3.rssa.visitor.RSSAVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link DataflowProblem} instance used to determine, whether a procedure changes (writes to)
 * memory, when it is called. Since a procedure either can directly execute an {@link Instruction}
 * writing to memory or can call another procedure, that executes such an {@link Instruction},
 * it is possible to model this property as a {@link DataflowProblem}.
 * <p>
 * The {@link DataflowProblem} itself is modelled on the {@link CallDependencyGraph}
 * of a {@link Program} together with a {@link Lattice} operating on
 * {@link Boolean} values.
 */
public final class MemoryManipulatingProcedures extends DataflowProblem<String, Boolean> {

    /**
     * Returns a {@link Map} that gives a {@link Boolean} value for every procedure
     * in {@link Program}, describing whether this procedure directly or indirectly
     * changes values in memory.
     */
    public static Map<String, Boolean> detectProcedures(Program program) {
        return new MemoryManipulatingProcedures(program, CallDependencyGraph.constructGraph(program))
                .solve().getIn();
    }

    private final Set<String> memoryManipulatingProcedures;

    private MemoryManipulatingProcedures(Program program, CallDependencyGraph graph) {
        super(graph.allProcedures(), graph::getCallingProcedures, graph::getCalledProcedures,
                Direction.BACKWARD);

        memoryManipulatingProcedures = cacheNamesOfMemoryManipulatingProcedures(program);
    }

    @Override
    public Boolean initialInValue(String node) {
        return BOOLEAN_LATTICE.bottom();
    }

    @Override
    public Boolean initialOutValue(String node) {
        return memoryManipulatingProcedures.contains(node);
    }

    @Override
    public Boolean combine(Boolean a, Boolean b) {
        return BOOLEAN_LATTICE.join(a, b);
    }

    @Override
    public Boolean transfer(Boolean value, String node) {
        return value;
    }


    /**
     * The standard {@link Boolean} {@link Lattice} with a single bit of
     * information.
     */
    private static final Lattice<Boolean> BOOLEAN_LATTICE = new Lattice<>() {
        @Override
        public Boolean top() {
            return true;
        }

        @Override
        public Boolean bottom() {
            return false;
        }

        @Override
        public Boolean join(Boolean a, Boolean b) {
            return a || b;
        }

        @Override
        public Boolean meet(Boolean a, Boolean b) {
            return a && b;
        }
    };


    private Set<String> cacheNamesOfMemoryManipulatingProcedures(Program program) {
        Set<String> result = new HashSet<>(program.procedures().size());
        for (Map.Entry<String, ControlGraph> procedure : program.procedures().entrySet()) {
            if (anyInstructionManipulatesMemory(procedure.getValue().toCode())) {
                result.add(procedure.getKey());
            }
        }
        return result;
    }

    private boolean anyInstructionManipulatesMemory(List<Instruction> instructions) {
        return instructions.stream().anyMatch(INSTRUCTION_MANIPULATES_MEMORY::apply);
    }

    /**
     * Return <code>true</code> for the overridden methods and <code>false</code> otherwise.
     */
    private static final RSSAVisitor<Boolean> INSTRUCTION_MANIPULATES_MEMORY = new DoNothingRSSAVisitor<>(false) {
        @Override
        public Boolean visit(MemoryAssignment memoryAssignment) {
            return true;
        }

        @Override
        public Boolean visit(MemoryInterchangeInstruction memoryInterchangeInstruction) {
            return true;
        }

        @Override
        public Boolean visit(MemorySwapInstruction memorySwapInstruction) {
            return true;
        }
    };
}
