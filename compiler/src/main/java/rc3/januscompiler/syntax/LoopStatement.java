package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents reversible loops in the abstract syntax tree.
 * <p>
 * Syntax of {@link LoopStatement}:
 * <pre>
 *      LoopStatement ::= 'from' {@link Expression} 'do' StatementList 'loo' StatementList 'until' {@link Expression}
 *
 *      StatementList ::= '' | {@link Statement} StatementList
 * </pre>
 * </p>
 */
public final class LoopStatement extends Statement {
    /**
     * The {@link Expression} evaluated at the beginning of the loop used to ensure injectivity.
     */
    public final Expression fromCondition;
    /**
     * The {@link Expression} used to determine how many repetitions of the loop should be executed.
     */
    public final Expression untilCondition;
    /**
     * The first body of the loop.
     */
    public final List<Statement> doStatements;
    /**
     * The second body of the loop.
     */
    public final List<Statement> loopStatements;

    public LoopStatement(Position position, Expression fromCondition, List<Statement> doStatements,
                         List<Statement> loopStatements, Expression untilCondition) {
        super(position);
        this.fromCondition = fromCondition;
        this.untilCondition = untilCondition;
        this.doStatements = doStatements;
        this.loopStatements = loopStatements;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("LoopStatement",
                formatMember("condition (FROM)", String.valueOf(fromCondition)),
                formatMember("statements (DO)", doStatements),
                formatMember("statements (LOOP)", loopStatements),
                formatMember("condition (UNTIL)", String.valueOf(untilCondition)));
    }
}
