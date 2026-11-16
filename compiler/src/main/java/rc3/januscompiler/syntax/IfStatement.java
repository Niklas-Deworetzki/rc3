package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents reversible branches in the abstract syntax tree.
 * <p>
 * Syntax of {@link IfStatement}:
 * <pre>
 *      IfStatement ::= 'if' {@link Expression} 'then' StatementList 'else' StatementList 'fi' {@link Expression}
 *
 *      StatementList ::= '' | {@link Statement} StatementList
 * </pre>
 * </p>
 */
public final class IfStatement extends Statement {
    /**
     * The boolean {@link Expression} used to decide which branch is to be executed.
     */
    public final Expression condition;
    /**
     * The branch executed if the condition is <code>true</code>.
     */
    public final List<Statement> thenStatements;
    /**
     * The branch executed if the condition is <code>false</code>.
     */
    public final List<Statement> elseStatements;
    /**
     * The boolean {@link Expression} used to validate the injectiveness of the executed branch.
     */
    public final Expression assertion;

    public IfStatement(Position position, Expression condition, List<Statement> thenStatements,
                       List<Statement> elseStatements, Expression assertion) {
        super(position);
        this.condition = condition;
        this.thenStatements = thenStatements;
        this.elseStatements = elseStatements;
        this.assertion = assertion;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("IfStatement",
                formatTree("condition", condition),
                formatTree("then branch", thenStatements),
                formatTree("else branch", elseStatements),
                formatTree("assertion", assertion));
    }
}
