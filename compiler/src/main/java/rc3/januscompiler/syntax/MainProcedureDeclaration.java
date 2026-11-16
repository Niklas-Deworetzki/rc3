package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Identifier;
import rc3.lib.parsing.Position;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class represents the special procedure declaration with the name 'main' in the abstract syntax tree.
 * <p>
 * Syntax of the {@link MainProcedureDeclaration}:
 * <pre>
 *      MainProcedureDeclaration ::= 'procedure' 'main' '(' ')' LocalDeclarationList StatementList
 *
 *      LocalDeclarationList ::= '' | {@link VariableDeclaration} LocalDeclarationList
 *
 *      StatementList ::= '' | {@link Statement} StatementList
 * </pre>
 * </p>
 */
public final class MainProcedureDeclaration extends ProcedureDeclaration {
    public static final String NAME = "main";

    /**
     * The locally declared variables in the procedure's body.
     */
    public final List<VariableDeclaration> variables;

    public MainProcedureDeclaration(Position position, List<VariableDeclaration> variables, List<Statement> body) {
        // MainProcedureDeclaration is always initialized as a procedure named "main" with empty parameter list.
        // This reflects the restrictions present in the grammar of Janus.
        super(position, new Identifier(NAME, position), emptyList(), body);
        this.variables = variables;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("MainProcedureDeclaration",
                name,
                formatMember("parameter list", parameters),
                formatMember("variable list", variables),
                formatMember("body", body));
    }
}
