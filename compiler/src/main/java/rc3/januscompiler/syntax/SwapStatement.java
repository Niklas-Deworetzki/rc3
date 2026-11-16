package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

/**
 * This class represents all swapping assignment statements in the abstract syntax tree.
 * <p>
 * Syntax of {@link SwapStatement}:
 * <pre>
 *      SwapStatement ::= {@link Variable} '<=>' {@link Variable}
 * </pre>
 * </p>
 */
public final class SwapStatement extends Statement {
    /**
     * The left-hand side of this statement.
     */
    public final Variable lhsvar;
    /**
     * The right-hand side of this statement.
     */
    public final Variable rhsvar;

    public SwapStatement(Position position, Variable lhsvar, Variable rhsvar) {
        super(position);
        this.lhsvar = lhsvar;
        this.rhsvar = rhsvar;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("SwapStatement",
                formatMember("left variable", String.valueOf(lhsvar)),
                formatMember("right variable", String.valueOf(rhsvar)));
    }
}
