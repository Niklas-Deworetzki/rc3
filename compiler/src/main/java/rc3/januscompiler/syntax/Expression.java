package rc3.januscompiler.syntax;

import rc3.januscompiler.pass.TypeCalculatingVisitor;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.types.Type;
import rc3.lib.parsing.Position;

/**
 * This abstract superclass represents all expressions in Janus.
 */
public abstract sealed class Expression extends Tree
        permits BinaryExpression, BuiltinExpression, IntLiteral, StackNilLiteral, VariableExpression {
    /**
     * The {@link Type} of the expression. It is populated upon the first call of
     * {@link Expression#getDataType(Environment)}.
     *
     * @apiNote This field is populated during local semantic analysis.
     */
    public Type dataType = null;

    protected Expression(Position position) {
        super(position);
    }

    /**
     * Calculates and returns the {@link Type} of this {@link Expression}.
     * <p>
     * The first call of this method calculates the {@link Type} and stores it. Since the {@link Type}
     * of an {@link Expression} is not expected to change, the {@link Type} can be accessed in constant
     * time afterwards.
     * </p>
     *
     * @param environment The {@link Environment} under which the {@link Type} is calculated.
     * @return The {@link Type} of this {@link Expression}.
     */
    public Type getDataType(Environment environment) {
        if (dataType == null) {
            dataType = this.accept(new TypeCalculatingVisitor(environment));
        }
        return dataType;
    }
}
