package rc3.rssa.instances;

import rc3.rssa.visitor.RSSAVisitor;

import java.util.List;
import java.util.Set;

import static rc3.lib.utils.SetOperations.concat;

/**
 * A conditional entry point
 * <pre>
 *  l1(x, ...)l2 <- c
 * </pre>
 * where <var>c</var> is a condition, <var>l1</var> and <var>l2</var> are labels and the <var>x</var>s are atoms.
 * If <var>x</var> is a variable, it is created and given the value of the corresponding parameter of the
 * incoming label. If entered through a jump to <var>l1</var>, the condition <var>c</var> is evaluated and
 * verified to be true. If entered through a jump to <var>l2</var>, the condition <var>c</var> is evaluated
 * and is verified to be false.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 */
public final class ConditionalEntry extends ControlInstruction implements ControlInstruction.Conditional {
    public String trueLabel;
    public String falseLabel;
    public RValue left;
    public BinaryOperand.BinaryOperator operator;
    public RValue right;

    public ConditionalEntry(String trueLabel, String falseLabel, RValue left, BinaryOperand.BinaryOperator operator, RValue right) {
        super(true);
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
        this.left = left.copy();
        this.operator = operator;
        this.right = right.copy();

        assert operator.isValidInCondition() : "Conditional operator required!";
    }

    public ConditionalEntry(String trueLabel, String falseLabel, BinaryOperand condition) {
        this(trueLabel, falseLabel, condition.left, condition.operator, condition.right);
    }

    @Override
    public BinaryOperand getCondition() {
        return new BinaryOperand(left, operator, right);
    }

    @Override
    public String trueLabel() {
        return trueLabel;
    }

    @Override
    public String falseLabel() {
        return falseLabel;
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        right = right.replace(original, replacement);
        left = left.replace(original, replacement);
    }

    @Override
    public ConditionalExit reverse() {
        ConditionalExit result = new ConditionalExit(trueLabel, falseLabel, left.copy(), operator, right.copy());
        result.parameters.addAll(this.copyParameters());
        return copyReversedAnnotations(result);
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        return concat(super.variablesUsedInInstruction(),
                left.variablesUsed(), right.variablesUsed());
    }

    @Override
    public Set<String> associatedLabels() {
        return Set.of(trueLabel, falseLabel);
    }

    @Override
    public String getRepresentation() {
        return String.format("%s(%s)%s <- %s %s %s",
                trueLabel,
                formatParameterList(),
                falseLabel,
                left, operator, right);
    }
}
