package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

/**
 * This class represents a target for a {@link JumpingInstruction}.
 */
public class Label extends Instruction {
    public final String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s:", name);
    }
}
