package rc3.rssa;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.optimization.Optimization;
import rc3.lib.utils.SetOperations;
import rc3.rssa.blocks.CallDependencyGraph;
import rc3.rssa.instances.Program;

import java.util.Collection;
import java.util.HashSet;

public class UnusedProcedureElimination extends Optimization {
    Program originalProgram;

    public UnusedProcedureElimination(Compiler compiler, Program program) {
        super(AvailableOptimization.UNUSED_PROCEDURES_ELIMINATION, compiler.getOptimizationState());
        this.originalProgram = program;
    }

    public void performOptimization() {
        Collection<String> uncalledProcedures = SetOperations.difference(originalProgram.procedureNames(),
                new HashSet<>(CallDependencyGraph.constructGraph(originalProgram)
                        .getAllCalledProcedures(originalProgram.getEntryPoint())));
        // Note: The entry point won't be marked as called, since it is the root if program execution,
        // but of course needs to be executed
        uncalledProcedures.remove(originalProgram.getEntryPoint());
        uncalledProcedures.forEach(proc -> originalProgram.procedures().remove(proc));
    }
}
