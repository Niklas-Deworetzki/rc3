package rc3.rssa.visitor;

import rc3.rssa.instances.*;

/**
 * This class is a specialization of {@link RSSAVisitor} for a <code>void</code> return type.
 * <p>
 * It would be required to <code>return null;</code> in every implemented method of the visitor,
 * so a <code>visitVoid</code> method is provided for every <code>visit</code> method of the tree visitor.
 * </p>
 */
public abstract class VoidRSSAVisitor extends RSSAVisitor<Void> {

    @Override
    public Void visit(ArithmeticAssignment arithmeticInstruction) {
        visitVoid(arithmeticInstruction);
        return null;
    }

    @Override
    public Void visit(BeginInstruction beginInstruction) {
        visitVoid(beginInstruction);
        return null;
    }

    @Override
    public Void visit(BinaryOperand binaryOperand) {
        visitVoid(binaryOperand);
        return null;
    }

    @Override
    public Void visit(CallInstruction callInstruction) {
        visitVoid(callInstruction);
        return null;
    }

    @Override
    public Void visit(ConditionalEntry conditionalEntry) {
        visitVoid(conditionalEntry);
        return null;
    }

    @Override
    public Void visit(ConditionalExit conditionalExit) {
        visitVoid(conditionalExit);
        return null;
    }

    @Override
    public Void visit(Constant constant) {
        visitVoid(constant);
        return null;
    }

    @Override
    public Void visit(EndInstruction endInstruction) {
        visitVoid(endInstruction);
        return null;
    }

    @Override
    public Void visit(MemoryAccess memoryAccess) {
        visitVoid(memoryAccess);
        return null;
    }

    @Override
    public Void visit(MemoryAssignment memoryAssignment) {
        visitVoid(memoryAssignment);
        return null;
    }

    @Override
    public Void visit(MemoryInterchangeInstruction memoryInterchangeInstruction) {
        visitVoid(memoryInterchangeInstruction);
        return null;
    }

    @Override
    public Void visit(MemorySwapInstruction memorySwapInstruction) {
        visitVoid(memorySwapInstruction);
        return null;
    }

    @Override
    public Void visit(SwapInstruction swapInstruction) {
        visitVoid(swapInstruction);
        return null;
    }

    @Override
    public Void visit(UnconditionalEntry unconditionalEntry) {
        visitVoid(unconditionalEntry);
        return null;
    }

    @Override
    public Void visit(UnconditionalExit unconditionalExit) {
        visitVoid(unconditionalExit);
        return null;
    }

    @Override
    public Void visit(Variable variable) {
        visitVoid(variable);
        return null;
    }
    

    protected abstract void visitVoid(BeginInstruction beginInstruction);

    protected abstract void visitVoid(ArithmeticAssignment arithmeticInstruction);

    protected abstract void visitVoid(BinaryOperand binaryOperand);

    protected abstract void visitVoid(CallInstruction callInstruction);

    protected abstract void visitVoid(ConditionalEntry conditionalEntry);

    protected abstract void visitVoid(ConditionalExit conditionalExit);

    protected abstract void visitVoid(Constant constant);

    protected abstract void visitVoid(EndInstruction endInstruction);

    protected abstract void visitVoid(MemoryAccess memoryAccess);

    protected abstract void visitVoid(MemoryAssignment memoryAssignment);

    protected abstract void visitVoid(MemoryInterchangeInstruction memoryInterchangeInstruction);

    protected abstract void visitVoid(MemorySwapInstruction memorySwapInstruction);

    protected abstract void visitVoid(SwapInstruction swapInstruction);

    protected abstract void visitVoid(UnconditionalEntry unconditionalEntry);

    protected abstract void visitVoid(UnconditionalExit unconditionalExit);

    protected abstract void visitVoid(Variable variable);
}
