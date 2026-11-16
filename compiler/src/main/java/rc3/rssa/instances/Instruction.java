package rc3.rssa.instances;

import rc3.lib.parsing.Position;
import rc3.lib.utils.CollectionUtils;
import rc3.rssa.annotations.Annotatable;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.SourceView;
import rc3.rssa.visitor.VisitableRSSA;

import java.util.*;

import static rc3.lib.utils.SetOperations.concat;

/**
 * This abstract superclass of all RSSA {@link Instruction Instructions} implements common methods, that
 * can be used within all subclasses.
 */
public sealed abstract class Instruction implements VisitableRSSA, Annotatable
        permits ArithmeticAssignment, CallInstruction, ControlInstruction, MemoryAssignment, MemoryInterchangeInstruction, MemorySwapInstruction, SwapInstruction {
    /**
     * The {@link Set} of {@link Annotation Annotations} this {@link Instruction} is annotated with.
     */
    private final Set<Annotation> annotations = new HashSet<>(0 /* We rarely use any Annotations */);

    private Instruction reversed = null;

    @Override
    public final Set<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * All {@link Annotation Annotations} present on this {@link Instruction} are copied onto the given
     * {@link Instruction} of type <var>I</var>. While copying, the {@link Annotation Annotations} are
     * reversed so that the target holds all reversed {@link Annotation Annotations}.
     * <p>
     * This method does not mutate the called-on object.
     *
     * @return Returns the given {@link Instruction}.
     */
    protected <I extends Instruction> I copyReversedAnnotations(I instruction) {
        for (Annotation annotation : annotations) {
            instruction.annotate(annotation.reverse());
        }
        return instruction;
    }

    /**
     * Mutates this {@link Instruction} to replace all contained {@link Variable Variables} with the given
     * replacement. This method can be used to implement constant propagation.
     * <p>
     * If this {@link Instruction} does not contain any occurrence of the given {@link Variable} it remains
     * unchanged.
     *
     * @param original    The {@link Variable} to replace.
     * @param replacement The {@link Atom} used as an replacement.
     */
    public void replaceVariable(Variable original, Atom replacement) {
        List<Annotation> updatedAnnotations = new ArrayList<>(annotations.size());

        for (Annotation annotation : annotations) {
            updatedAnnotations.add(annotation.replaceVariable(original, replacement));
        }

        annotations.clear();
        updatedAnnotations.forEach(this::annotate);
    }

    /**
     * Accepts an arbitrary amount of {@link Atom Atoms} and returns a {@link List} of all
     * {@link Variable Variables} that were part of the passed {@link Atom Atoms}.
     */
    protected static List<Variable> atomsAsVariableList(Atom... atoms) {
        List<Variable> result = new ArrayList<>();
        for (Atom atom : atoms) {
            if (atom instanceof Variable) {
                result.add((Variable) atom);
            }
        }
        return result;
    }

    /**
     * Reverses this {@link Instruction}.
     */
    public abstract Instruction reverse();

    public Instruction invert() {
        if (reversed == null) {
            reversed = reverse();
            reversed.reversed = this;
        }
        return reversed;
    }

    /**
     * Creates a copy of this {@link Instruction}.
     */
    public Instruction copy() {
        return reverse().reverse();
    }

    /**
     * Lists all {@link Variable Variables} that are created by this {@link Instruction}.
     * <p>
     * During forwards execution, every {@link Variable} created can be used after this instruction was executed.
     */
    public abstract List<Variable> variablesCreated();

    /**
     * Lists all {@link Variable Variables} that are destroyed by this {@link Instruction}.
     * <p>
     * During backwards execution, every {@link Variable} destroyed can not be used anymore after this instruction
     * was executed.
     */
    public abstract List<Variable> variablesDestroyed();

    /**
     * Returns the union of {@link Instruction#variablesUsed()} and
     * {@link Instruction#variablesDestroyed()}.
     */
    public final List<Variable> variablesUsedOrDestroyed() {
        return concat(variablesUsed(),
                variablesDestroyed());
    }

    /**
     * List all {@link Variable Variables} that are used by this instruction.
     * <p>
     * A {@link Variable} is used, if it is <b>neither</b> destroyed <b>nor</b> created by this
     * {@link Instruction} but occurs as part of the {@link Instruction} any other way.
     */
    public final List<Variable> variablesUsed() {
        return concat(variablesUsedInInstruction(),
                findVariablesInAnnotations());
    }

    /**
     * Returns a {@link List} of all {@link Variable} instances used within this instruction that are
     * <b>not</b> destroyed by it. This <b>includes</b> all variables used in {@link Annotation Annotations}
     * of operands but <b>excludes</b> all variables used in {@link Annotation Annotations} present
     * on this {@link Instruction}.
     */
    protected abstract List<Variable> variablesUsedInInstruction();

    /**
     * Returns a representation of this {@link Instruction}.
     */
    protected abstract String getRepresentation();

    @Override
    public final String toString() {
        return getRepresentation() + formatAnnotations();
    }

    /**
     * Returns the {@link Position} acquired by {@link Instruction#findPosition(Annotatable)}
     * or {@link Position#NONE}, if no such position could be found.
     */
    public Position getPosition() {
        return findPosition(this).orElse(Position.NONE);
    }

    /**
     * Tries to acquire the {@link Position} for a given {@link Annotatable annotated RSSA instance}.
     *
     * @return The {@link Position} of the given instance, if present.
     */
    public static Optional<Position> findPosition(Annotatable annotatable) {
        return annotatable.findAnnotation("Source")
                .map(annotation -> ((SourceView) annotation.getView()).getPosition());
    }

    /**
     * Inverts a {@link List} of {@link Instruction} instances, inverting the {@link List}
     * order and calling {@link Instruction#reverse()} for every item.
     */
    public static List<Instruction> reverseInstructions(List<Instruction> instructions) {
        List<Instruction> result = new ArrayList<>(instructions.size());
        for (Instruction instruction : CollectionUtils.reverse(instructions)) {
            result.add(instruction.reverse());
        }
        return result;
    }
}
