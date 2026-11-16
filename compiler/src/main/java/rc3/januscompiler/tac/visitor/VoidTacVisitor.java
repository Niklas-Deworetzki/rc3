package rc3.januscompiler.tac.visitor;

import rc3.januscompiler.tac.instances.*;

/**
 * This class is a specialization of {@link TacVisitor} for a <code>void</code> return type.
 * <p>
 * It would be required to <code>return null;</code> in every implemented method of the visitor,
 * so a <code>visitVoid</code> method is provided for every <code>visit</code> method of the tree visitor.
 * </p>
 */
public abstract class VoidTacVisitor extends TacVisitor<Void> {

    @Override
    public Void visit(AddressOfInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(ArithmeticInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(BeginInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(CallInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(ConstantOperand operand) {
        visitVoid(operand);
        return null;
    }

    @Override
    public Void visit(CopyInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(EndInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(GotoInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(IfGotoInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(Label label) {
        visitVoid(label);
        return null;
    }

    @Override
    public Void visit(LoadInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(NamedOperand operand) {
        visitVoid(operand);
        return null;
    }

    @Override
    public Void visit(ParamInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(ReturnInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(StoreInstruction instruction) {
        visitVoid(instruction);
        return null;
    }

    @Override
    public Void visit(TemporaryOperand operand) {
        visitVoid(operand);
        return null;
    }

    public abstract void visitVoid(AddressOfInstruction instruction);

    public abstract void visitVoid(ArithmeticInstruction instruction);

    public abstract void visitVoid(BeginInstruction instruction);

    public abstract void visitVoid(CallInstruction instruction);

    public abstract void visitVoid(ConstantOperand operand);

    public abstract void visitVoid(CopyInstruction instruction);

    public abstract void visitVoid(EndInstruction instruction);

    public abstract void visitVoid(GotoInstruction instruction);

    public abstract void visitVoid(IfGotoInstruction instruction);

    public abstract void visitVoid(Label label);

    public abstract void visitVoid(LoadInstruction instruction);

    public abstract void visitVoid(NamedOperand operand);

    public abstract void visitVoid(ParamInstruction instruction);

    public abstract void visitVoid(ReturnInstruction instruction);

    public abstract void visitVoid(StoreInstruction instruction);

    public abstract void visitVoid(TemporaryOperand operand);

}
