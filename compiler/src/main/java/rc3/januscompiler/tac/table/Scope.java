package rc3.januscompiler.tac.table;

import rc3.januscompiler.syntax.LocalBlockStatement;
import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.tac.instances.Instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class holds information about the different {@link Variable Variables} used in a
 * {@link ProcedureDeclaration}.
 * <p>
 * In a {@link ProcedureDeclaration} the scoping information of local variables
 * is implicitly stored in the structure of the AST. This information would be lost when generating TAC
 * {@link Instruction Instructions}. To preserve this information
 * this class used.
 * </p>
 */
public class Scope {
    public final Variable variable;
    public final List<Scope> children = new ArrayList<>();

    public Scope(Variable variable) {
        this.variable = variable;
    }

    /**
     * Returns the {@link Variable} that was used in the {@link LocalBlockStatement}
     * to initialize a local scope.
     *
     * @return {@link Variable} at the root of the scope.
     */
    public Variable getVariable() {
        return variable;
    }

    /**
     * Returns all {@link Scope Scopes} that this {@link Scope} contains.
     *
     * @return A {@link List} of all contained {@link Scope Scopes}.
     */
    public List<Scope> getChildren() {
        return children;
    }

    /**
     * Returns all {@link Variable Variables} in this {@link Scope} and every contained {@link Scope}.
     *
     * @return A {@link List} of all {@link Variable Variables} in this {@link Scope} and contained {@link Scope Scopes}
     */
    public List<Variable> allVariables() {
        List<Variable> resultList = new ArrayList<>();
        resultList.add(variable);
        for (Scope child : getChildren()) {
            resultList.addAll(child.allVariables());
        }
        return resultList;
    }

    @Override
    public String toString() {
        return String.join("\n", variable.toString(),
                children.stream()
                        .map(String::valueOf)
                        .map(str -> " " + str).collect(Collectors.joining("\n")));
    }
}
