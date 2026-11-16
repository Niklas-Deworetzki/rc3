package rc3.rssa.cse;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.Program;

import java.util.Map;

/**
 * Implements Common Subexpression Elimination as an encapsulated {@link Program.ProcedureOptimization}.
 */
public final class CommonSubexpressionElimination extends Program.ProcedureOptimization {
    private final Map<String, Boolean> memoryManipulatingProcedures;

    public CommonSubexpressionElimination(Compiler compiler, Program program) {
        super(AvailableOptimization.COMMON_SUBEXPRESSION_ELIMINATION, compiler);
        this.memoryManipulatingProcedures = MemoryManipulatingProcedures.detectProcedures(program);
    }

    private BasicBlock localCSE(BasicBlock block) {
        CSEGraph cseGraph = new CSEGraph(block, memoryManipulatingProcedures);
        debug(" Graph for " + block.graphId());
        debug(cseGraph::graphString);
        return block.copyWithNewBody(CSECodeGenerator.defaultGenerator(cseGraph).generateCode());
    }

    @Override
    public ControlGraph apply(String procedureName, ControlGraph originalGraph) {
        debug("\n\n======= CSE on " + procedureName);
        debug("\n Code before CSE:");
        debug(originalGraph::graphString);

        debug("\nCSE (fw):");
        ControlGraph optimizedFw = originalGraph.updated(this::localCSE);
        debug(" Code after CSE (fw):");
        debug(optimizedFw::graphString);

        debug("\nCSE (bw):");
        ControlGraph optimizedBw = optimizedFw.reversed().updated(this::localCSE);
        debug(" Code after CSE (bw):");
        debug(optimizedBw::graphString);

        return optimizedBw.reversed();
    }
}
