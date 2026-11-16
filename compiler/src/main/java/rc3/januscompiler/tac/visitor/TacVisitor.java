package rc3.januscompiler.tac.visitor;

import rc3.januscompiler.tac.instances.*;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This abstract class is a skeleton for concrete {@link TacVisitor} instances.
 * <p>
 * {@link Consumer} and {@link Function} is implemented by this abstract class, so visitor instances can be
 * used with modern functional Java constructs.
 * </p>
 *
 * @param <R> The type of the value returned by visiting an {@link VisitableTac}.
 */
public abstract class TacVisitor<R> implements Consumer<VisitableTac>, Function<VisitableTac, R> {

    @Override
    public void accept(VisitableTac visitable) {
        visitable.accept(this);
    }

    @Override
    public R apply(VisitableTac visitable) {
        return visitable.accept(this);
    }


    public abstract R visit(AddressOfInstruction instruction);

    public abstract R visit(ArithmeticInstruction instruction);

    public abstract R visit(BeginInstruction instruction);

    public abstract R visit(CallInstruction instruction);

    public abstract R visit(ConstantOperand operand);

    public abstract R visit(CopyInstruction instruction);

    public abstract R visit(EndInstruction instruction);

    public abstract R visit(GotoInstruction instruction);

    public abstract R visit(IfGotoInstruction instruction);

    public abstract R visit(Label label);

    public abstract R visit(LoadInstruction instruction);

    public abstract R visit(NamedOperand operand);

    public abstract R visit(ParamInstruction instruction);

    public abstract R visit(ReturnInstruction instruction);

    public abstract R visit(StoreInstruction instruction);

    public abstract R visit(TemporaryOperand operand);
}
