package rc3.rssa.instances;

import rc3.rssa.blocks.VariableIndexer;
import rc3.rssa.pass.StackAllocation;
import rc3.rssa.visitor.RSSAVisitor;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static rc3.lib.utils.StringUtils.formatNumberWithAlphabet;

/**
 * Variables are named values in RSSA, that can only be assigned once.
 * <p>
 * To ensure that every variable is only assigned to once, even tough a variable can occur multiple times
 * in the source code, variables are indexed with an unique incrementing number. The combination of name and
 * number is then to be assigned to only once per subroutine.
 * </p>
 * <p>
 * This class holds required fields to represent both behaviors a variable must express:
 * <ul>
 *  <li>It has a {@link Variable#name} that can be used to refer to the variable.</li>
 *  <li>It has an index {@link Variable#number} that is used to ensure single assignability.</li>
 * </ul>
 * Additionally, this class provides a static method {@link Variable#nextTemporaryVariable()} to create
 * new "fresh" variables, that can be used to hold temporary results.
 * </p>
 *
 * @see VariableIndexer
 */
public final class Variable extends Atom {
    public static final int UNINITIALIZED = -1;

    private final String name;
    public int number = UNINITIALIZED;

    /**
     * Describes a {@link Variable Variables} offset within a stack frame.
     */
    public int offset = StackAllocation.NOT_ALLOCATED;

    public Variable(String name) {
        this.name = requireNonNull(name, "Variable name can't be null!");
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Atom replace(Variable original, Atom replacement) {
        Atom replaced = this;
        if (this.equals(original)) {
            replaced = replacement;
            replaced.annotate(getAnnotations());
        }

        replaced.replaceVariablesInAnnotations(original, replacement);
        return replaced;
    }

    @Override
    public Variable copy() {
        Variable result = new Variable(name);
        result.number = this.number;
        result.offset = this.offset;
        return copyAnnotations(result);
    }

    /**
     * Returns the name of this {@link Variable} together with a numerical suffix,
     * if this variable was indexed previously.
     */
    public String getName() {
        if (number < 0) {
            return name;
        } else {
            return String.format("%s_%d", name, number);
        }
    }

    /**
     * Returns the raw name of this {@link Variable} without a numerical suffix.
     */
    public String getRawName() {
        return name;
    }

    /**
     * Returns the version number of this {@link Variable}.
     * <p>
     * In SSA (and therefore by extension RSSA too), every {@link Variable} is only assigned to
     * once. If the value of a {@link Variable} is modified multiple times over the course of
     * a procedure, every assignment is annotated with a version number. These numbers ensure
     * the SSA property be making every assignment unique, since the combination of number and
     * name defines a {@link Variable}.
     */
    public int getVersionNumber() {
        return number;
    }

    @Override
    public String formatAtom() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Variable other)) return false;

        return this.number == other.number &&
                this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return hash(name, number);
    }

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static int temporaryVariableVersion = 0;

    private static String nextTemporaryName() {
        return "T" + formatNumberWithAlphabet(temporaryVariableVersion++, ALPHABET);
    }

    /**
     * Creates a new temporary variable with a fresh name.
     */
    public static Variable nextTemporaryVariable() {
        return new Variable(nextTemporaryName());
    }
}
