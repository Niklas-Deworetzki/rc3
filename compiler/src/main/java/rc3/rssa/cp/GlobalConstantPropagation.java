package rc3.rssa.cp;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.lib.dataflow.ConstantLattice;
import rc3.lib.dataflow.DataflowProblem;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.VariableRenamer;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static rc3.lib.utils.CollectionUtils.*;

/**
 * This class implements Constant Propagation over multiple {@link BasicBlock}
 * as an optimization in RSSA.
 * <p>
 * The optimization is implemented as a {@link DataflowProblem}, assigning either
 * a {@link ConstantLattice.Constant} value, {@link ConstantLattice#top()}, or
 * {@link ConstantLattice#bottom()} to every parameter in a {@link BasicBlock}'s
 * parameter list. The {@link BasicBlock#outputParameters() output parameter lists}
 * of a block's predecessors are combined to obtain the block's input list.
 * The {@link DataflowProblem#transfer(Object, Object) transfer function} follows
 * assignments within a {@link BasicBlock}, propagating eventual constants to
 * its output.
 */
public final class GlobalConstantPropagation extends Program.ProcedureOptimization {
    private final LocalConstantPropagation lcp;

    private static final ConstantLattice<Integer> lattice = ConstantLattice.instance();

    public GlobalConstantPropagation(Compiler compiler) {
        super(AvailableOptimization.GLOBAL_CONSTANT_PROPAGATION, compiler);
        this.lcp = new LocalConstantPropagation(compiler);
    }

    @Override
    public ControlGraph apply(String s, ControlGraph graph) {
        final AtomicBoolean propagatedConstants = new AtomicBoolean();

        do {
            propagatedConstants.set(false);

            graph = lcp.apply(s, graph); // Optimize each block in both directions.
            graph = runOptimization(graph, propagatedConstants).reversed();
            graph = runOptimization(graph, propagatedConstants).reversed();

        } while (propagatedConstants.get());
        return graph;
    }

    private ControlGraph runOptimization(ControlGraph graph, AtomicBoolean propagatedConstants) {
        final var solution = new GCPAnalysis(lattice, graph).solve();

        for (BasicBlock block : graph) {
            // Combine result of analysis with parameters already present.
            final List<Atom> newParameters = zipWith(block.inputParameters(), solution.getIn(block),
                    (parameter, value) -> {
                        if (parameter instanceof Constant)
                            return parameter; // Don't change, if param already is a constant.
                        // Create Atom from constant value, if present. Otherwise return parameter unchanged.
                        return value.<Atom>map(Constant::new).orElse(parameter);
                    }
            ).toList();

            final List<Atom> previousParameters = List.copyOf(block.inputParameters());
            if (!previousParameters.equals(newParameters)) { // Update if changed.
                propagatedConstants.set(true);
                final var renamer = VariableRenamer.fromParameterLists(previousParameters, newParameters);
                for (Instruction instruction : block) {
                    renamer.accept(instruction);
                }
            }
        }

        return graph;
    }

    /**
     * {@link DataflowProblem} for Global Constant Propagation.
     * <p>
     * Every input parameter starts as {@link ConstantLattice#top()} while every
     * output parameter starts as {@link ConstantLattice#bottom()}.
     */
    private static final class GCPAnalysis extends DataflowProblem<BasicBlock, List<ConstantLattice.Value<Integer>>> {
        private final ConstantLattice<Integer> lattice;

        public GCPAnalysis(ConstantLattice<Integer> lattice, ControlGraph procedure) {
            super(procedure.getNodes(), procedure::getPredecessors, procedure::getSuccessors, Direction.FORWARD);
            this.lattice = lattice;
        }

        @Override
        public List<ConstantLattice.Value<Integer>> initialInValue(BasicBlock node) {
            return fillList(lattice.top(), node.inputParameters().size());
        }

        @Override
        public List<ConstantLattice.Value<Integer>> initialOutValue(BasicBlock node) {
            return fillList(lattice.top(), node.outputParameters().size());
        }

        @Override
        public List<ConstantLattice.Value<Integer>> combine(List<ConstantLattice.Value<Integer>> a, List<ConstantLattice.Value<Integer>> b) {
            return zipWith(a, b, lattice::meet).toList(); // Combine predecessors output by element-wise application of meet.
        }

        @Override
        public List<ConstantLattice.Value<Integer>> transfer(List<ConstantLattice.Value<Integer>> input, BasicBlock node) {
            final Map<Variable, ConstantLattice.Value<Integer>> environment = new HashMap<>();

            // Fill environment with passed values.
            final List<Atom> inputParameters = node.inputParameters();
            for (int index = 0; index < inputParameters.size(); index++) {
                if (inputParameters.get(index) instanceof Variable variable) {
                    environment.put(variable, input.get(index));
                }
            }

            final List<Atom> outputParameters = node.outputParameters();
            final List<ConstantLattice.Value<Integer>> result = new ArrayList<>(outputParameters.size());
            // Reconstruct parameter list.
            for (Atom outputParameter : outputParameters) {
                if (outputParameter instanceof Constant constant) {
                    result.add(lattice.lift(constant.value));
                } else {
                    result.add(environment.getOrDefault((Variable) outputParameter, lattice.bottom()));
                }
            }

            return result;
        }
    }
}