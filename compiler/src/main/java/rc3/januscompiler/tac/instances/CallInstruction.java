package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.TacVisitor;

public class CallInstruction extends JumpingInstruction {

    public CallInstruction(String label) {
        super(label);
    }

    @Override
    public <R> R accept(TacVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("\tCall %s", label);
    }
}
