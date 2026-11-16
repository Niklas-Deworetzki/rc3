package rc3.rssa.visitor;

import rc3.rssa.instances.*;

/**
 * This class implements every <code>visitVoid</code> method with an empty effect.
 */
public class DoNothingVoidRSSAVisitor extends VoidRSSAVisitor {
    @Override
    protected void visitVoid(BeginInstruction beginInstruction) {

    }

    @Override
    protected void visitVoid(ArithmeticAssignment arithmeticInstruction) {

    }

    @Override
    protected void visitVoid(BinaryOperand binaryOperand) {

    }

    @Override
    protected void visitVoid(CallInstruction callInstruction) {

    }

    @Override
    protected void visitVoid(ConditionalEntry conditionalEntry) {

    }

    @Override
    protected void visitVoid(ConditionalExit conditionalExit) {

    }

    @Override
    protected void visitVoid(Constant constant) {

    }

    @Override
    protected void visitVoid(EndInstruction endInstruction) {

    }

    @Override
    protected void visitVoid(MemoryAccess memoryAccess) {

    }

    @Override
    protected void visitVoid(MemoryAssignment memoryAssignment) {

    }

    @Override
    protected void visitVoid(MemoryInterchangeInstruction memoryInterchangeInstruction) {

    }

    @Override
    protected void visitVoid(MemorySwapInstruction memorySwapInstruction) {

    }

    @Override
    protected void visitVoid(SwapInstruction swapInstruction) {

    }

    @Override
    protected void visitVoid(UnconditionalEntry unconditionalEntry) {

    }

    @Override
    protected void visitVoid(UnconditionalExit unconditionalExit) {

    }

    @Override
    protected void visitVoid(Variable variable) {

    }
}
