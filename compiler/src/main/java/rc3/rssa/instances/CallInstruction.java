package rc3.rssa.instances;

import rc3.januscompiler.Direction;
import rc3.rssa.visitor.RSSAVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An instruction of the form
 * <pre>
 * (x, . . .) := call l(y,...)
 * </pre>
 * or
 * <pre>
 * (x, . . .) := uncall l(y,...)
 * </pre>
 * <p>
 * where <var>l</var> is a label and the <var>x</var>s and <var>y</var>s are atoms. The subroutine labelled
 * <var>l</var> is called. The values of the <var>x</var>s are passed as arguments to the subroutine. If an
 * <var>x</var> is a variable, it is destroyed. At return, the variables <var>y</var> are created and given
 * the values passed back by the subroutine.
 * <p>
 * If {@link Direction#BACKWARD} is used, the <code>uncall</code> variant is executed as follows:
 * Run the subroutine <var>l</var> backwards, starting at <code>end l(y,...)</code> and finishing at
 * <code>begin l(x,...)</code>. Semantically, this corresponds to performing <code>(x, . . .) := call l(y,...)</code>
 * in the inverted program.
 *
 * @implSpec T. Mogensen, RSSA: A Reversible SSA Form, S. 213. In: Perspectives of System Informatics 2015
 * @implNote <code>call</code> and <code>uncall</code> variant implemented as a single class.
 */
public final class CallInstruction extends Instruction {
    public final List<Atom> outputList = new ArrayList<>();
    public final Direction direction;
    public final String procedure;
    public final List<Atom> inputList = new ArrayList<>();

    public CallInstruction(List<? extends Atom> outputList, Direction direction, String procedure, List<? extends Atom> inputList) {
        this.direction = direction;
        this.procedure = procedure;

        for (Atom atom : outputList) {
            this.outputList.add(atom.copy());
        }
        for (Atom atom : inputList) {
            this.inputList.add(atom.copy());
        }
    }

    @Override
    public void replaceVariable(Variable original, Atom replacement) {
        super.replaceVariable(original, replacement);
        inputList.replaceAll(atom -> atom.replace(original, replacement));
        outputList.replaceAll(atom -> atom.replace(original, replacement));
    }

    @Override
    public CallInstruction reverse() {
        return copyReversedAnnotations(
                new CallInstruction(
                        inputList,
                        direction.invert(), procedure,
                        outputList));
    }

    @Override
    public <R> R accept(RSSAVisitor<R> visitor) {
        return visitor.visit(this);
    }


    @Override
    public List<Variable> variablesCreated() {
        return atomsAsVariableList(outputList.toArray(Atom[]::new));
    }

    @Override
    public List<Variable> variablesDestroyed() {
        return atomsAsVariableList(inputList.toArray(Atom[]::new));
    }

    @Override
    public List<Variable> variablesUsedInInstruction() {
        List<Variable> result = new ArrayList<>();
        for (Atom atom : inputList) {
            result.addAll(atom.findVariablesInAnnotations());
        }
        for (Atom atom : outputList) {
            result.addAll(atom.findVariablesInAnnotations());
        }

        return result;
    }

    @Override
    public String getRepresentation() {
        return String.format("(%s) := %s %s(%s)",
                formatList(outputList),
                direction.asCallKeyword(), procedure,
                formatList(inputList));
    }

    private static String formatList(List<? extends Atom> atomsList) {
        return atomsList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
