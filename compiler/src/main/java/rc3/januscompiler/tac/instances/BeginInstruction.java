package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.table.Procedure;
import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This {@link Instruction} represents the start of a {@link Procedure}.
 * <p>
 * A translation of this {@link Instruction} to executable code has to prepare {@link Procedure Procedure's}
 * stack frame.
 * </p>
 */
public class BeginInstruction extends Instruction {
    public final String procedureLabel;
    public final Procedure procedure;

    public BeginInstruction(String procedureLabel, Procedure procedure) {
        this.procedureLabel = procedureLabel;
        this.procedure = procedure;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\tBegin %s", procedureLabel);
    }

}
