package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;

import static rc3.januscompiler.syntax.AssignStatement.ModificationOperator;
import static rc3.lib.utils.SetOperations.concat;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator;

/**
 * Represents an instruction of the form
 * <pre>
 *   x := y ⊕ R1 ⊙ R2
 * </pre>
 * where <var>x</var> and <var>y</var> are atoms and <var>R1</var> and <var>R2</var> are R-values. The value
 * <var>w</var> of <code>R1 ⊕ R2</code> is calculated and the value <var>v</var> of <var>y</var> is fetched.
 * If <var>y</var> is a variable, it is destroyed. If <var>x</var> is a variable,
 * it is created and given the value <code>u = v ⊕ w</code>.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class ArithmeticAssignment extends Instruction {
    public Atom target;
    public Atom source;
    public ModificationOperator operator;
    public RValue left;
    public BinaryOperator arithmeticOperator;
    public RValue right;

    public ArithmeticAssignment(Atom target, Atom source, ModificationOperator operator,
                                RValue left, BinaryOperator arithmeticOperator, RValue right) {
        this.target = target.copy();
        this.source = source.copy();
        this.operator = operator;
        this.left = left.copy();
        this.arithmeticOperator = arithmeticOperator;
        this.right = right.copy();

        // FIXME: To evaluate complex conditions, we currently require boolean expressions in assignments.
        // assert arithmeticOperator.isArithmetic() : "Arithmetic operator required.";
        /*
         * if a > 0 || b < 2
         * --------------------
         * ta := 0 ^ (a > 0)
         * tb := 0 ^ (b < 2)
         * tc := 0 ^ (ta || tc)
         */
    }

    public ArithmeticAssignment(Atom target, Atom source, ModificationOperator operator, BinaryOperand value) {
        this(target, source, operator, value.left, value.operator, value.right);
    }

    /**
     * Creates an assignment with the given <var>source</var> and <var>target</var> {@link Atom Atoms} and
     * a {@link Value}. The value is inspected to decide how the instruction is created:
     * <ul>
     *  <li>The {@link Value} is a {@link BinaryOperand}:
     *  <p>Operands and operator of the {@link BinaryOperand} are extracted and passed to the constructor.</p>
     *  </li>
     *  <li>The {@link Value} is <b>not</b> a {@link BinaryOperand}:
     *  <p>An arithmetic identity is added (<var>+ 0</var> ) so all constructor parameters are occupied.</p>
     *  </li>
     * </ul>
     *
     * <pre>
     *  Target := Source ± Value
     * </pre>
     */
    public static ArithmeticAssignment newInstruction(Atom target, Atom source, ModificationOperator operator, Value value) {
        if (value instanceof BinaryOperand binary) {
            return new ArithmeticAssignment(target, source, operator,
                    binary.left, binary.operator, binary.right);
        } else {
            return newInstruction(target, source, operator, BinaryOperand.of((RValue) value));
        }
    }

    /**
     * Creates an assignment with the given <var>source</var> and <var>target</var> {@link Atom Atoms}.
     * The <var>source</var> is destroyed.
     *
     * <pre>
     *  Target := Source ^ 0 + 0
     * </pre>
     */
    public static ArithmeticAssignment assignAndDestroy(Atom target, Atom source) {
        return new ArithmeticAssignment(target, source, ModificationOperator.XOR, // Use XOR to distinguish from arithmetic
                new Constant(0), BinaryOperator.XOR, new Constant(0));
    }

    /**
     * Creates an assignment with the given <var>source</var> and <var>target</var> {@link Atom Atoms}.
     * The <var>source</var> is <b>not</b> destroyed.
     *
     * <pre>
     *  Target := 0 + Source + 0
     * </pre>
     */
    public static ArithmeticAssignment assignWithoutDestroy(Atom target, BinaryOperand source) {
        return new ArithmeticAssignment(target, new Constant(0), ModificationOperator.XOR, source);
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        target = target.replace(original, replacement);
        source = source.replace(original, replacement);
        left = left.replace(original, replacement);
        right = right.replace(original, replacement);
    }

    @Override
    public ArithmeticAssignment reverse() {
        return copyReversedAnnotations(
                new ArithmeticAssignment(
                        source.copy(), target.copy(), operator.invert(),
                        left.copy(), arithmeticOperator, right.copy()));
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    /**
     * Returns the complete value, that is used to perform this assignment.
     *
     * <p>
     * In an assignment of the form <code>a := b + (x * y)</code> the value returned by
     * this method is a {@link BinaryOperand} holding <code>(x + y)</code>.
     * </p>
     */
    public BinaryOperand getValue() {
        return new BinaryOperand(left, arithmeticOperator, right);
    }

    @Override
    public List<Variable> variablesCreated() {
        if (target instanceof Variable) {
            return List.of((Variable) target);
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesDestroyed() {
        if (source instanceof Variable) {
            return List.of((Variable) source);
        } else {
            return List.of();
        }
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        return concat(target.findVariablesInAnnotations(), source.findVariablesInAnnotations(),
                left.variablesUsed(), right.variablesUsed());
    }

    @Override
    protected String getRepresentation() {
        return String.format("%s := %s %s (%s %s %s)",
                target, source, operator, left, arithmeticOperator, right);
    }
}
