package rc3.rssa.instances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A superclass of all entry and exit points for a basic block.
 */
public sealed abstract class ControlInstruction extends Instruction
        permits BeginInstruction, ConditionalEntry, ConditionalExit, EndInstruction, UnconditionalEntry, UnconditionalExit {
    public final boolean isEntryPoint;
    protected final List<Atom> parameters = new ArrayList<>();

    protected ControlInstruction(boolean isEntryPoint) {
        this.isEntryPoint = isEntryPoint;
    }

    /**
     * Creates an explicit {@link Atom#copy() copy} of every parameter in this
     * instruction's parameter list and collects them into a new {@link List}.
     */
    public List<Atom> copyParameters() {
        return parameters.stream()
                .map(Atom::copy)
                .collect(Collectors.toList());
    }

    /**
     * Returns the underlying {@link List} of parameters. Any change on this
     * {@link List} is reflected in this instruction.
     */
    public List<Atom> getParameters() {
        return parameters;
    }

    /**
     * Updates the underlying {@link List} of parameters, inserting the
     * given elements. The reference returned by {@link ControlInstruction#getParameters()}
     * is not changed, only its contents are updated.
     * <p>
     * This method returns <code>true</code>, if any parameters changed during this operation.
     */
    public boolean setParameters(List<? extends Atom> parameters) {
        if (parameters.size() != this.parameters.size()) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
            return true;
        }

        boolean parameterWasUpdated = false;
        for (int i = 0; i < parameters.size(); i++) {
            final Atom newParam = parameters.get(i);
            final Atom oldParam = this.parameters.set(i, newParam);

            parameterWasUpdated = parameterWasUpdated || !newParam.equals(oldParam);
        }
        return parameterWasUpdated;
    }

    public ControlInstruction withParameters(List<? extends Atom> parameters) {
        setParameters(parameters);
        return this;
    }

    public void addParameters(Collection<? extends Atom> parameters) {
        parameters.forEach(this::addParameter);
    }

    public void addParameter(Atom parameter) {
        this.parameters.add(parameter.copy());
    }

    public boolean isEntryPoint() {
        return isEntryPoint;
    }

    public boolean isExitPoint() {
        return !isEntryPoint;
    }

    public abstract Set<String> associatedLabels();

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        parameters.replaceAll(param -> param.replace(original, replacement));
    }

    @Override
    public List<Variable> variablesCreated() {
        if (isEntryPoint()) {
            return variablesInParameters();
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesDestroyed() { // TODO: Is this correct? Mogensen doesn't explicitly say they are destroyed, but it does not make sense otherwise.
        if (isExitPoint()) {
            return variablesInParameters();
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        List<Variable> result = new ArrayList<>();
        for (Atom parameter : parameters) { // Variables in Annotations count as "used" but are not created or destroyed.
            result.addAll(parameter.findVariablesInAnnotations());
        }
        return result;
    }

    private List<Variable> variablesInParameters() {
        return atomsAsVariableList(parameters.toArray(Atom[]::new));
    }

    protected String formatParameterList() {
        return parameters.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }


    /**
     * Return <code>true</code>, if this {@link ControlInstruction} is a {@link Conditional}.
     */
    public boolean isConditional() {
        return this instanceof Conditional;
    }

    /**
     * Return <code>true</code>, if this {@link ControlInstruction} is a {@link Unconditional}.
     */
    public boolean isUnconditional() {
        return this instanceof Unconditional;
    }


    /**
     * Common interface for all unconditional variants of entry and exit points.
     */
    public sealed interface Unconditional permits UnconditionalEntry, UnconditionalExit {
        /**
         * Returns the label associated with the entry or exit point.
         */
        String label();
    }

    /**
     * Common interface for all conditional variants of entry and exit points.
     */
    public sealed interface Conditional permits ConditionalEntry, ConditionalExit {
        /**
         * Returns the label associated with a <code>true</code> condition.
         */
        String trueLabel();

        /**
         * Returns the label associated with a <code>false</code> condition.
         */
        String falseLabel();

        /**
         * Returns the {@link BinaryOperand binary expression} representing this
         * instruction's condition.
         */
        BinaryOperand getCondition();

        /**
         * Returns the {@link Conditional#trueLabel()} or {@link Conditional#falseLabel()},
         * depending on whether the input was <code>true</code> or <code>false</code>.
         */
        default String getLabel(boolean condition) {
            return (condition) ? trueLabel() : falseLabel();
        }
    }
}
