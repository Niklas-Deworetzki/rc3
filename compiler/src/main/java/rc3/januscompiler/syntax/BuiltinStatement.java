package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Builtins;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents {@link Statement Statements} that are builtin into Janus.
 * <p>
 * These statements are modelled as a called procedure, since any effectful computation has to receive
 * arguments, that it should operate on. These arguments are passed as the argument list.
 * </p>
 *
 * @see Builtins.Procedure for available Procedures that are bultin into Janus.
 */
public final class BuiltinStatement extends Statement {
    /**
     * The called {@link Builtins.Procedure}.
     */
    public final Builtins.Procedure builtinProcedure;
    /**
     * The argument list provided to the builtin.
     */
    public final List<Variable> argumentList;

    public BuiltinStatement(Position position, Builtins.Procedure builtinProcedure, List<Variable> argumentList) {
        super(position);
        this.builtinProcedure = builtinProcedure;
        this.argumentList = argumentList;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
