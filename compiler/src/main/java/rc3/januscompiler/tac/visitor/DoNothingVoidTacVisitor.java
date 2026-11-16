package rc3.januscompiler.tac.visitor;

import rc3.januscompiler.tac.instances.*;

/**
 * This class implements every <code>visitVoid</code> method with an empty effect.
 */
public class DoNothingVoidTacVisitor extends VoidTacVisitor {
    @Override
    public void visitVoid(AddressOfInstruction instruction) {

    }

    @Override
    public void visitVoid(ArithmeticInstruction instruction) {

    }

    @Override
    public void visitVoid(BeginInstruction instruction) {

    }

    @Override
    public void visitVoid(CallInstruction instruction) {

    }

    @Override
    public void visitVoid(ConstantOperand operand) {

    }

    @Override
    public void visitVoid(CopyInstruction instruction) {

    }

    @Override
    public void visitVoid(EndInstruction instruction) {

    }

    @Override
    public void visitVoid(GotoInstruction instruction) {

    }

    @Override
    public void visitVoid(IfGotoInstruction instruction) {

    }

    @Override
    public void visitVoid(Label label) {

    }

    @Override
    public void visitVoid(LoadInstruction instruction) {

    }

    @Override
    public void visitVoid(NamedOperand operand) {

    }

    @Override
    public void visitVoid(ParamInstruction instruction) {

    }

    @Override
    public void visitVoid(ReturnInstruction instruction) {

    }

    @Override
    public void visitVoid(StoreInstruction instruction) {

    }

    @Override
    public void visitVoid(TemporaryOperand operand) {

    }
}
