package rc3.rssa.inlining;

import rc3.lib.utils.CollectionUtils;
import rc3.rssa.blocks.CallDependencyGraph;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.CallInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static rc3.lib.utils.CollectionUtils.makeUnique;

/**
 * The default {@link InliningStrategy} used by the optimizing RSSA backend.
 * <p>
 * This strategy selects three distinct variants procedure calls as targets for inlining:
 * <ol>
 *  <li>Calls to small procedures are selected for inlining, if the number of instructions within the called
 *  procedure is below a certain threshold. The default value of this is represented by the constant
 *  {@link DefaultInliningStrategy#DEFAULT_MAXIMAL_INLINE_SIZE}.</li>
 *  <li>Calls to procedures that are only called once within the whole program are always selected for inlining,
 *  since this removes the call overhead and the procedure's definition is unused afterwards and could be removed.</li>
 *  <li>Calls to procedures that the user always wants to inline.</li>
 * </ol>
 * <p>
 * The process of selecting inline targets is customizable by the user, as the maximal inlining size can be overwritten
 * by a user defined value and a list of procedure names can be passed, that should always or never be inlined.
 * When passing names of procedures that should always or never be inlined, the order of names does matter, as
 * a procedure that is blacklisted, can be whitelisted later and the other way around.
 * When specifying the size of procedures, that should be inlined, this strategy always uses the maximum passed value
 * and will only resort to the default value, if no size was passed at all.
 * <p>
 * When selecting inline targets, the order of call sites is usually selected by traversing the program in topological
 * order. This means that the main procedure is analyzed last, as it is the root of the program flow, while procedures
 * that do not contain any more calls, are analyzed first. This way, it is ensured that inlining has the most effect
 * as procedures in which calls were inlined could be inlined themselves afterwards.
 * <p>
 * The only exception to this order is the order of user defined procedures. Here the order specified order is used
 * to schedule targets.
 */
class DefaultInliningStrategy extends InliningStrategy {
    /**
     * The maximal amount of instructions a procedure is allowed to have to consider calls of it available for inlining.
     */
    public static final int DEFAULT_MAXIMAL_INLINE_SIZE = 14;

    // While the order of procedures that should be inlined is relevant, the set of never inlined procedures has to have fast lookup times.
    private final Collection<String> alwaysInline = new ArrayList<>();
    private final Collection<String> neverInline = new HashSet<>();
    private int maximalInliningSize = DEFAULT_MAXIMAL_INLINE_SIZE;

    // Cache a graph holding procedure calls and the topological order of a program.
    private final CallDependencyGraph dependencyGraph = CallDependencyGraph.constructGraph(getProgram());
    private final List<String> topologicalOrder = CollectionUtils.topologicalOrder(dependencyGraph.allProcedures(), dependencyGraph::getCalledProcedures);

    public DefaultInliningStrategy(Program program) {
        super(program);
    }

    @Override
    public void dumpConfiguration(PrintStream dumpStream) {
        System.out.printf("%s:%n", DefaultInliningStrategy.class.getSimpleName());
        System.out.printf(" Maximal procedure size: %d%n", maximalInliningSize);
        System.out.printf(" Whitelisted procedures: %s%n", String.join(", ", alwaysInline));
        System.out.printf(" Blacklisted procedures: %s%n", String.join(", ", neverInline));
    }

    @Override
    protected void setupOptions(List<String> options) {
        List<Integer> inliningSizes = new LinkedList<>();

        for (String option : options) { // Iterate options in order since it is relevant for configuration and inlining.
            OptionalInt numericValue = numericValue(option);
            if (numericValue.isPresent()) {
                inliningSizes.add(numericValue.getAsInt());

            } else if (option.startsWith("!")) {
                String procedureName = option.substring(1);
                neverInline.add(procedureName);
                alwaysInline.remove(procedureName);

            } else {
                alwaysInline.add(option);
                neverInline.remove(option);
            }
        }

        this.maximalInliningSize = inliningSizes.stream().mapToInt(Integer::intValue)
                .max().orElse(DEFAULT_MAXIMAL_INLINE_SIZE);
    }

    private static OptionalInt numericValue(String string) {
        try {
            return OptionalInt.of(Integer.parseInt(string));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private static Predicate<Instruction> isCallTo(String procedure) {
        return instruction -> instruction instanceof CallInstruction &&
                ((CallInstruction) instruction).procedure.equals(procedure);
    }

    // Creates an InliningSite that holds all calls to "procedureToInline" within "caller".
    private InliningSite populateSimpleSite(String caller, String procedureToInline) {
        InliningSite site = new InliningSite(caller);
        getProgram().getProcedure(caller).toCode()
                .stream().filter(isCallTo(procedureToInline))
                .forEach(site::addInlineableInstruction);
        return site;
    }

    @Override
    protected void fillInliningQueue() {
        enqueueSmallProcedures();
        enqueueProceduresCalledOnce();
        enqueueAlwaysInlinedProcedures();
    }

    /**
     * Iterate user defined procedure names and enqueue all call-sites for these procedures.
     */
    protected void enqueueAlwaysInlinedProcedures() {
        for (String inlinedProcedure : alwaysInline) {
            for (String caller : makeUnique(dependencyGraph.getCallingProcedures(inlinedProcedure))) {
                InliningSite site = populateSimpleSite(caller, inlinedProcedure);
                enqueueSite(site);
            }
        }
    }

    /**
     * Enqueue all call sites for procedures that are only called once.
     */
    protected void enqueueProceduresCalledOnce() {
        for (String procedureToInline : topologicalOrder) {
            Collection<String> callingProcedures = dependencyGraph.getCallingProcedures(procedureToInline);

            if (callingProcedures.size() == 1 && !dependencyGraph.isRecursive(procedureToInline)
                    && !neverInline.contains(procedureToInline)) {
                String caller = CollectionUtils.anyElement(callingProcedures);

                InliningSite site = populateSimpleSite(caller, procedureToInline);
                enqueueSite(site);
            }
        }
    }

    // Get the sizes for each procedures wrapped in a mutable object.
    private Map<String, AtomicInteger> getProcedureSizes() {
        Map<String, AtomicInteger> procedureSizes = new HashMap<>();
        for (Map.Entry<String, ControlGraph> entry : getProgram().procedures().entrySet()) {
            procedureSizes.put(entry.getKey(), new AtomicInteger(entry.getValue().codeSize()));
        }
        return procedureSizes;
    }

    /**
     * Enqueue all call sites for small procedures.
     */
    protected void enqueueSmallProcedures() {
        Map<String, AtomicInteger> procedureSizes = getProcedureSizes();

        for (String procedureToInline : topologicalOrder) {
            if (neverInline.contains(procedureToInline)) continue; // Skip blacklisted procedures.

            int inlinedProcedureSize = procedureSizes.get(procedureToInline).get();
            if (inlinedProcedureSize > maximalInliningSize) continue; // Procedure is too large to inline.

            Map<String, Integer> occurrences = CollectionUtils.countOccurrences(dependencyGraph.getCallingProcedures(procedureToInline));
            for (Map.Entry<String, Integer> occurrence : occurrences.entrySet()) {
                if (occurrence.getKey().equals(procedureToInline)) continue; // Skip recursive procedures.

                int cost = occurrence.getValue() * inlinedProcedureSize;
                if (cost <= maximalInliningSize) {
                    procedureSizes.get(occurrence.getKey()).addAndGet(cost);

                    InliningSite site = populateSimpleSite(occurrence.getKey(), procedureToInline);
                    enqueueSite(site);
                }
            }
        }
    }
}