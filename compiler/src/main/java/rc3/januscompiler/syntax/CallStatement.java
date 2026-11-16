package rc3.januscompiler.syntax;

import rc3.januscompiler.Direction;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents procedure calls and uncalls in the abstract syntax tree.
 * <p>
 * Syntax of {@link CallStatement}:
 * <pre>
 *      CallStatement ::= Direction {@link Identifier} '(' ArgumentList ')'
 *
 *      Direction ::= 'call' | 'uncall'
 *
 *      ArgumentList ::= '' | {@link Variable} ArgumentListTail
 *
 *      ArgumentListTail ::= '' | ',' {@link Variable} ArgumentListTail
 * </pre>
 * </p>
 */
public final class CallStatement extends Statement {
    /**
     * The direction of the call.
     */
    public final Direction direction;
    /**
     * The name of the called procedure.
     */
    public final Identifier procedureName;
    /**
     * The list of passed arguments.
     */
    public final List<Variable> argumentList;

    public CallStatement(Position position, Direction direction, Identifier procedureName, List<Variable> argumentList) {
        super(position);
        this.direction = direction;
        this.procedureName = procedureName;
        this.argumentList = argumentList;
    }

    /**
     * This method is used to determine, whether this {@link CallStatement} represents a <b>call</b>, keeping the
     * execution direction.
     *
     * @return <code>true</code> if {@link CallStatement#direction} equals {@link Direction#FORWARD}.
     */
    public boolean isCall() {
        return direction.isForward();
    }

    /**
     * This method is used to determine, whether this {@link CallStatement} represents an <b>uncall</b>,
     * inverting the execution direction.
     *
     * @return <code>true</code> if {@link CallStatement#direction} equals {@link Direction#BACKWARD}.
     */
    public boolean isUncall() {
        return direction.isBackward();
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("CallStatement",
                direction.asCallKeyword(),
                procedureName,
                formatMember("argument list", argumentList));
    }
}
