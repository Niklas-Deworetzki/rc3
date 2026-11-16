package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents declared procedures in the abstract syntax tree.
 * <p>
 * Syntax of {@link ProcedureDeclaration}:
 * <pre>
 *      ProcedureDeclaration ::= 'procedure' {@link Identifier} '(' ParameterList ')' StatementList
 *
 *      ParameterList ::= '' | {@link VariableDeclaration} ParameterList
 *
 *      StatementList ::= '' | {@link Statement} StatementList
 * </pre>
 * </p>
 */
public sealed class ProcedureDeclaration extends Declaration<ProcedureEntry>
        permits MainProcedureDeclaration {
    /**
     * The list of parameters declared for this procedure.
     */
    public final List<VariableDeclaration> parameters;
    /**
     * The statements contained in the body of this procedure.
     */
    public List<Statement> body;

    public ProcedureDeclaration(Position position, Identifier name, List<VariableDeclaration> parameters,
                                List<Statement> body) {
        super(position, name);
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("ProcedureDeclaration",
                name,
                formatMember("parameter list", parameters),
                formatMember("body", body));
    }
}
