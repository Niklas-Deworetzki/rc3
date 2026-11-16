package rc3.januscompiler.syntax;

import rc3.januscompiler.pass.TypeCalculatingVisitor;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.types.Type;
import rc3.lib.parsing.Position;

/**
 * This abstract class represents all variables in the abstract syntax tree.
 * <p>
 * Variables are identified by an {@link Identifier} and have a {@link Type} depending
 * on the {@link Environment} they are in.
 * </p>
 */
public abstract sealed class Variable extends Tree permits IndexedVariable, NamedVariable {
    /**
     * The name of the variable.
     */
    public final Identifier name;
    /**
     * The {@link Type} of the variable.
     */
    public Type dataType;

    public Variable(Position position, Identifier name) {
        super(position);
        this.name = name;
    }

    /**
     * Calculates and returns the {@link Type} of this {@link Variable}.
     * <p>
     * The first call of this method calculates the {@link Type} and stores it. Since the {@link Type}
     * of an {@link Variable} is not expected to change, the {@link Type} can be accessed in constant
     * time afterwards.
     * </p>
     *
     * @param environment The {@link Environment} under which the {@link Type} is calculated.
     * @return The {@link Type} of this {@link Variable}.
     */
    public Type getDataType(Environment environment) {
        if (dataType == null) {
            dataType = this.accept(new TypeCalculatingVisitor(environment));
        }
        return dataType;
    }
}
