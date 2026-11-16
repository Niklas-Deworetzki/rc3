package rc3.rssa.inlining;

import rc3.lib.messages.ErrorMessage;
import rc3.rssa.instances.CallInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Instances of this class decide the order in which inlining opportunities are taken advantage of.
 * <p>
 * If you want to implement a concrete instance of this class, two methods should be overridden:
 * <ul>
 *  <li>{@link InliningStrategy#setupOptions(List)} is used to configure this strategy with options that
 *  were passed by the user. This method is called once after instance creation, before any scheduling
 *  should be performed.</li>
 *  <li>{@link InliningStrategy#fillInliningQueue()} is called to perform scheduling. {@link InliningSite}
 *  instances are created during scheduling and are added to the inlining queue via
 *  {@link InliningStrategy#enqueueSite(InliningSite)}</li>
 * </ul>
 */
public abstract class InliningStrategy {
    protected final Program program;
    protected final Queue<InliningSite> inliningSites = new LinkedList<>();
    private boolean isInitialized = false, isEnqueued = false;

    public InliningStrategy(Program program) {
        this.program = program;
    }

    /**
     * Initializes this {@link InliningStrategy} instance with a set of given options.
     * <p>
     * This method internally calls {@link InliningStrategy#setupOptions(List)} which is to be overridden
     * by concrete instances.
     */
    public final void initializeStrategy(List<String> options) {
        try {
            setupOptions(options);
        } catch (IllegalArgumentException illegalOptions) {
            String description = illegalOptions.getMessage() != null ? illegalOptions.getMessage()
                    : "Some options are invalid or not recognized.";
            throw new ErrorMessage.InternalError("Illegal inlining configuration: " + description);
        }
        this.isInitialized = true;
    }

    /**
     * Outputs the configuration of this {@link InliningStrategy} so it can be inspected and verified by a user.
     */
    public abstract void dumpConfiguration(PrintStream dumpStream);

    /**
     * Initializes this instance with a set of given options.
     *
     * @throws IllegalArgumentException (optionally) if some options are not recognized or are invalid.
     *                                  An implementation of this method may also ignore invalid or unknown options.
     */
    protected abstract void setupOptions(List<String> options) throws IllegalArgumentException;

    /**
     * This method is called once to populate the queue of {@link InliningSite InliningSites}. Individual sites
     * can be added via {@link InliningStrategy#enqueueSite(InliningSite)}.
     * <p>
     * Individual {@link InliningSite Sites} are enqueued as the order of inlining operations is relevant for the
     * resulting code.
     */
    protected abstract void fillInliningQueue();

    /**
     * Returns the {@link Program} for which inlining should be performed.
     */
    public final Program getProgram() {
        return program;
    }

    /**
     * Enqueues the given {@link InliningSite} so the inliner using this strategy will inline the given site.
     */
    protected void enqueueSite(InliningSite site) {
        inliningSites.add(site);
    }

    /**
     * Populates and returns a {@link Queue} of {@link InliningSite InliningSites} that is used to schedule the order
     * of inlining operations.
     * <p>
     * The {@link InliningStrategy} this method is called on must be initialized first via
     * {@link InliningStrategy#initializeStrategy(List)}.
     *
     * @throws IllegalStateException if this instance wasn't initialized first.
     * @see InliningStrategy#initializeStrategy(List)
     */
    public final Queue<InliningSite> getInliningQueue() throws IllegalStateException {
        if (!isInitialized)
            throw new IllegalStateException(this.getClass().getCanonicalName() + " must be initialized " +
                    "via initializeStrategy(List<String>) before inlining opportunities can be found!");

        if (!isEnqueued) {
            fillInliningQueue();
            isEnqueued = true;
        }
        return inliningSites;
    }

    /**
     * A data class to represent inlining opportunities. An {@link InliningSite} provides a procedure name
     * and a {@link List} of {@link CallInstruction CallInstructions} within this procedure that are available
     * for inlining.
     */
    public static class InliningSite {
        public final String procedureName;
        public final List<CallInstruction> inlineableInstructions = new ArrayList<>();

        public InliningSite(String procedureName) {
            this.procedureName = procedureName;
        }

        public String getProcedureName() {
            return procedureName;
        }

        public void addInlineableInstruction(Instruction instruction) {
            inlineableInstructions.add((CallInstruction) instruction);
        }

        public void addInlineableInstruction(CallInstruction callInstruction) {
            inlineableInstructions.add(callInstruction);
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        public boolean includes(Instruction instruction) {
            return inlineableInstructions.contains(instruction);
        }

        @Override
        public String toString() {
            return procedureName + inlineableInstructions.stream()
                    .map(callInstruction -> callInstruction.procedure)
                    .collect(Collectors.joining(",", "{", "}"));
        }
    }
}
