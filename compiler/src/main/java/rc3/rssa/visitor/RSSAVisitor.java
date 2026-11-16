package rc3.rssa.visitor;

import rc3.rssa.instances.*;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This abstract class is a skeleton for concrete {@link RSSAVisitor} instances.
 * <p>
 * {@link Consumer} and {@link Function} is implemented by this abstract class, so visitor instances can be
 * used with modern functional Java constructs.
 * </p>
 *
 * @param <R> The type of the value returned by visiting an {@link VisitableRSSA}.
 */
public abstract class RSSAVisitor<R> implements Consumer<VisitableRSSA>, Function<VisitableRSSA, R> {

    @Override
    public void accept(VisitableRSSA visitable) {
        visitable.accept(this);
    }

    @Override
    public R apply(VisitableRSSA visitable) {
        return visitable.accept(this);
    }


    public abstract R visit(ArithmeticAssignment arithmeticInstruction);

    public abstract R visit(BeginInstruction beginInstruction);

    public abstract R visit(BinaryOperand binaryOperand);

    public abstract R visit(CallInstruction callInstruction);

    public abstract R visit(ConditionalEntry conditionalEntry);

    public abstract R visit(ConditionalExit conditionalExit);

    public abstract R visit(Constant constant);

    public abstract R visit(EndInstruction endInstruction);

    public abstract R visit(MemoryAccess memoryAccess);

    public abstract R visit(MemoryAssignment memoryAssignment);

    public abstract R visit(MemoryInterchangeInstruction memoryInterchangeInstruction);

    public abstract R visit(MemorySwapInstruction memorySwapInstruction);

    public abstract R visit(SwapInstruction swapInstruction);

    public abstract R visit(UnconditionalEntry unconditionalEntry);

    public abstract R visit(UnconditionalExit unconditionalExit);

    public abstract R visit(Variable variable);
}
