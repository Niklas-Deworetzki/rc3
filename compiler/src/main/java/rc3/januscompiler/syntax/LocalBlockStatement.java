package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents local blocks in the abstract syntax tree.
 * <p>
 * Syntax of {@link LocalBlockStatement}:
 * <pre>
 *      LocalBlockStatement ::= 'local' {@link VariableDeclaration} '=' {@link Expression} StatementList
 *                              'delocal' {@link VariableDeclaration} '=' {@link Expression}
 *
 *      StatementList ::= '' | {@link Statement} StatementList
 * </pre>
 * </p>
 */
public final class LocalBlockStatement extends Statement {
    /**
     * The {@link VariableDeclaration} introducing a variable into the local scope.
     */
    public final VariableDeclaration localDeclaration;
    /**
     * The {@link Expression} used to initialize the local variable.
     */
    public final Expression localInitializer;
    /**
     * The {@link VariableDeclaration} eliminating a variable from the local scope.
     */
    public final VariableDeclaration delocalDeclaration;
    /**
     * The {@link Expression} used to validate the local variable.
     */
    public final Expression delocalInitializer;
    /**
     * The body of the local block in which the introduced variable is visible.
     */
    public final List<Statement> body;

    public LocalBlockStatement(Position position, VariableDeclaration localDeclaration, Expression localInitializer,
                               List<Statement> body,
                               VariableDeclaration delocalDeclaration, Expression delocalInitializer) {
        super(position);
        this.localDeclaration = localDeclaration;
        this.localInitializer = localInitializer;
        this.body = body;
        this.delocalDeclaration = delocalDeclaration;
        this.delocalInitializer = delocalInitializer;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("LocalBlockStatement",
                formatMember("local declaration", String.valueOf(localDeclaration)),
                formatMember("local initializer", String.valueOf(localInitializer)),
                formatMember ("body", body),
                formatMember("de-local declaration", String.valueOf(delocalDeclaration)),
                formatMember("de-local initializer", String.valueOf(delocalInitializer)));
    }
}
