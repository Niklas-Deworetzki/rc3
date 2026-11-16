package rc3.januscompiler.syntax;

import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.parsing.Position;

import java.util.List;

/**
 * This class represents the root of the abstract syntax tree.
 */
public final class Program extends Tree {
    /**
     * A list of all procedures declared in this program.
     */
    public final List<ProcedureDeclaration> procedures;

    public Program(Position position, List<ProcedureDeclaration> procedures) {
        super(position);
        this.procedures = procedures;
    }

    @Override
    public <R> R accept(TreeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatTree("Program",
                formatMember("procedures", String.valueOf(procedures)));
    }
}
