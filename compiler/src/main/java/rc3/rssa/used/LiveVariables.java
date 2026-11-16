package rc3.rssa.used;

import rc3.januscompiler.Direction;
import rc3.lib.utils.CollectionUtils;
import rc3.lib.utils.SetOperations;
import rc3.lib.utils.StringUtils;
import rc3.lib.dataflow.DataflowProblem;
import rc3.lib.dataflow.DataflowSolution;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.stream.Collectors;


    /*
    int x
int y

local int z = 3
{OUT: z nicht live - > kann local/delocal eliminieren}


z -= 3

call f(x,y)

if x=0
   y+=1
fi x=0
call f(x,y,z)


{IN: z nicht live}
z+=x+y
	{OUT: z nicht live}
delocal int z = x + y {IN=z nicht live}

Vorwärts (unten -> oben)
{IN: x1, y1) z1 := 0                                     { OUT: x1 y1 z1 live }
(x2, y2, z2) := call f(x1, y1, z1) { OUT: x1 y1 z1 live }
z3 := z2 + (x2 + y2)     { OUT: x2 y2 live }
0 := z3 ^ (x2 + y2)      { OUT: x2 y2 live }

Rückwärts (oben -> unten)
	OUT: { x1, y1 }
(1) z1 := 0                   // 0 := z1
	IN: { x1, y1 }

	OUT=IN1={x1, y1}
(2) (x2, y2, z2) := call f(x1, y1, z1)  // (x1, y1, z1) := call f(x2, y2, z2)
	IN: {x2,y2,z2}

	OUT: { x2,y2,z2}
(3) z3 := z2 + (x2 + y2)       // z2 := z3-(x2+y2)
	IN: {x2,y2,z3 }

	OUT: {x2,y2,z3 }
(4) 0 := z3 ^ (x2 + y2)        // z3:=x2+y2
	IN: { x2, y2}

procedure f(x,y,z)



IN = (OUT - DEF) U USE = { R1, R2 }
x := y + R1 * R2
OUT = { }

     */


public final class LiveVariables {

    public static void dumpLiveVariables(Program rssaProgram) {
        for (Map.Entry<String, ControlGraph> procedure : rssaProgram.procedures().entrySet()) {
            final var solution = liveVariableAnalysis(procedure.getValue());

            System.out.println(graphResultOfAnalysis(procedure.getValue(), solution));
        }
    }


    public static DataflowSolution<Instruction, Set<Variable>> liveVariableAnalysis(ControlGraph procedure) {
        final var solution = new ProcedureLevelAnalysis(procedure).solve();
        final Map<Instruction, Set<Variable>> in = new HashMap<>(), out = new HashMap<>();

        for (BasicBlock block : procedure.getNodes()) {
            final var localSolution = new BlockLevelAnalysis(block, solution.getOut(block)).getSolution();

            in.putAll(localSolution.getIn());
            out.putAll(localSolution.getOut());
        }

        return new DataflowSolution<>(in, out);
    }


    private static boolean manipulatesMemory(Instruction instruction) {
        return instruction instanceof CallInstruction || instruction instanceof MemoryAssignment
                || instruction instanceof MemoryInterchangeInstruction || instruction instanceof MemorySwapInstruction;
    }

    private static Set<Variable> use(Instruction instruction, Set<Variable> out) {
        if (manipulatesMemory(instruction) || // Instructions having a side-effect cannot be eliminated
                !SetOperations.intersection(out, Set.copyOf(instruction.variablesCreated())).isEmpty()) { // Instruction produces a Live variable, its predecessors are also live.
            return Set.copyOf(instruction.variablesUsedOrDestroyed());
        }

        return Set.copyOf(instruction.variablesUsed());
    }

    private static Set<Variable> def(Instruction instruction) {
        if (instruction instanceof ControlInstruction && ((ControlInstruction) instruction).isEntryPoint) {
            return Collections.emptySet();
        }

        return Set.copyOf(instruction.variablesCreated());
    }

    private static final class BlockLevelAnalysis {
        private final BasicBlock block;
        private final Map<Instruction, Set<Variable>> IN = new HashMap<>(), OUT = new HashMap<>();

        public BlockLevelAnalysis(BasicBlock block, List<Boolean> liveParameters) {
            this.block = block;
            setup(liveParameters);
            runAnalysis();
        }

        public DataflowSolution<Instruction, Set<Variable>> getSolution() {
            return new DataflowSolution<>(IN, OUT);
        }

        private void setup(List<Boolean> liveParameters) {
            Set<Variable> initialSet = new HashSet<>();
            CollectionUtils.combineWith(block.exitPoint().getParameters(), liveParameters, (parameter, isLive) -> {
                if (isLive && parameter instanceof Variable) {
                    initialSet.add((Variable) parameter);
                }
            });
            OUT.put(block.exitPoint(), initialSet);
        }

        private void runAnalysis() {
            Set<Variable> out = OUT.get(block.exitPoint());
            Set<Variable> in;
            for (Instruction instruction : CollectionUtils.reverse(block)) {
                OUT.put(instruction, out);
                in = SetOperations.union(use(instruction, out), SetOperations.difference(out, def(instruction)));
                IN.put(instruction, in);

                out = in;
            }
        }
    }

    private static final class ProcedureLevelAnalysis extends DataflowProblem<BasicBlock, List<Boolean>> {
        public ProcedureLevelAnalysis(ControlGraph graph) {
            super(graph.getNodes(), graph::getPredecessors, graph::getSuccessors, Direction.BACKWARD);
        }

        @Override
        public List<Boolean> initialInValue(BasicBlock node) {
            return CollectionUtils.fillList(false, node.exitPoint().getParameters().size());
        }

        @Override
        public List<Boolean> initialOutValue(BasicBlock node) {
            final ControlInstruction exitPoint = node.exitPoint();
            if (exitPoint instanceof EndInstruction) {
                return CollectionUtils.fillList(true, exitPoint.getParameters().size());
            }
            return CollectionUtils.fillList(false, exitPoint.getParameters().size());
        }

        @Override
        public List<Boolean> combine(List<Boolean> a, List<Boolean> b) {
            Boolean[] values = new Boolean[Math.max(a.size(), b.size())];
            Arrays.fill(values, false);
            for (int i = 0; i < a.size(); i++) {
                values[i] |= a.get(i);
            }
            for (int i = 0; i < b.size(); i++) {
                values[i] |= b.get(i);
            }
            return Arrays.asList(values);
        }

        @Override
        public List<Boolean> transfer(List<Boolean> value, BasicBlock node) {
            final ControlInstruction entryPoint = node.entryPoint();
            final Set<Variable> liveBeforeEntryPoint = new BlockLevelAnalysis(node, value).getSolution().getIn(entryPoint);
            return entryPoint.getParameters().stream().map(liveBeforeEntryPoint::contains).collect(Collectors.toList());
        }
    }

    private static String graphResultOfAnalysis(ControlGraph graph, DataflowSolution<Instruction, Set<Variable>> solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(graph.graphId()).append('{');
        sb.append("node[shape=box,fontname=courier];");

        for (BasicBlock block : graph.getNodes()) {
            sb.append(block.graphId())
                    .append("[label=\"Block ")
                    .append(block.graphId())
                    .append("\\n");

            for (Instruction instruction : block) {
                sb.append("IN = ").append(formatSet(solution.getIn(instruction))).append("\\n");
                sb.append(StringUtils.escape(instruction.toString())).append("\\l");
                sb.append("OUT = ").append(formatSet(solution.getOut(instruction))).append("\\n");
            }

            sb.append('"');
            if (block.equals(graph.getBegin()) || block.equals(graph.getEnd())) {
                sb.append(",peripheries=2"); // Visually highlight root.
            }
            sb.append("];");
        }
        for (BasicBlock block : graph.getNodes()) {
            for (BasicBlock successor : graph.getSuccessors(block)) {
                sb.append(block.graphId())
                        .append(" -> ")
                        .append(successor.graphId())
                        .append(';');
            }
            for (BasicBlock predecessor : graph.getPredecessors(block)) {
                sb.append(block.graphId())
                        .append(" -> ")
                        .append(predecessor.graphId())
                        .append("[style=dashed];");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String formatSet(Set<Variable> set) {
        return set.stream().map(Variable::getName).collect(Collectors.joining(", ", "{", "}"));
    }
}
