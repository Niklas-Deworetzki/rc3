package rc3.rssa.instances;

import rc3.rssa.visitor.VisitableRSSA;

import java.util.List;

/**
 * A superclass of all classes representing operands, that are used within {@link Instruction Instructions}.
 */
public sealed interface Value extends VisitableRSSA permits BinaryOperand, RValue {

    Value replace(Variable original, Atom replacement);

    Value copy();

    List<Variable> variablesUsed();

}
