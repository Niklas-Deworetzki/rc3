package rc3.januscompiler.tac.table;

import rc3.januscompiler.syntax.MainProcedureDeclaration;
import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.tac.instances.Instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * This class holds information about {@link ProcedureDeclaration ProcedureDeclarations}.
 * <p>
 * A {@link ProcedureDeclaration} usually stores the information about its parameters
 * and local variables inside the AST. This information would be lost when generating TAC
 * {@link Instruction Instructions}. To preserve this information this class used.
 * </p>
 * <p>
 * This class does not only store the {@link ProcedureEntry} of the original
 * {@link ProcedureDeclaration} but also stores a {@link List} of parameters and
 * local declarations.
 * </p>
 * <p>
 * {@link Procedure#parameters} is always empty when the {@link Procedure} represents the "main" procedure. In this
 * case however {@link Procedure#localDeclarations} may contain the variables declared in
 * {@link MainProcedureDeclaration#variables}.
 * Every other procedure has an empty {@link Procedure#localDeclarations} list but may contain variables in
 * {@link Procedure#parameters}.
 * </p>
 */
public class Procedure {
    public final ProcedureEntry entry;
    public final List<Variable> parameters;
    public final List<Variable> localDeclarations;
    public final List<Scope> body = new ArrayList<>();

    private final boolean isJanusMain;


    public Procedure(ProcedureEntry entry) {
        this.entry = entry;
        this.isJanusMain = entry.getStringName().equals("main");

        if (isJanusMain) {
            this.parameters = emptyList();
            this.localDeclarations = new ArrayList<>();
        } else {
            this.parameters = new ArrayList<>();
            this.localDeclarations = emptyList();
        }
    }

    /**
     * Checks if this {@link Procedure} represents the 'main' procedure of a Janus program.
     *
     * @return <code>true</code> if this {@link Procedure} represents the 'main' procedure.
     */
    public boolean isJanusMain() {
        return isJanusMain;
    }

    /**
     * Returns the {@link Scope Scopes} introduced in the body of the original
     * {@link ProcedureDeclaration}.
     *
     * @return Originally introduced {@link Scope Scopes}.
     */
    public List<Scope> getBody() {
        return body;
    }

    /**
     * Returns a {@link List} of variables declared in the declaration list of the "main" procedure. If this instance
     * does not represent the "main" procedure, this list is empty.
     *
     * @return A {@link List} of variables declared in this procedures declaration list if this procedure is the
     * "main" procedure. Otherwise an empty list is returned.
     */
    public List<Variable> getLocalDeclarations() {
        return localDeclarations;
    }

    /**
     * Returns a {@link List} of parameters declared in the parameter list of a procedure. The "main" procedure
     * is not allowed to have parameters. Therefore this method will return an empty list for the "main" procedure.
     *
     * @return A {@link List} of variables declared in the parameter list.
     */
    public List<Variable> getParameters() {
        return parameters;
    }

    /**
     * Returns the {@link ProcedureEntry} of this {@link Procedure}, holding all information from previous analysis
     * passes.
     *
     * @return The {@link ProcedureEntry} of this {@link Procedure}.
     */
    public ProcedureEntry getEntry() {
        return entry;
    }

    /**
     * @see ProcedureEntry#getParameterTypes()
     */
    public List<ParameterType> getParameterTypes() {
        return entry.getParameterTypes();
    }

    /**
     * @see ProcedureEntry#getLocalVariableAreaSize()
     */
    public int getLocalVariableAreaSize() {
        return entry.getLocalVariableAreaSize();
    }

    /**
     * @see ProcedureEntry#getParameterAreaSize()
     */
    public int getParameterAreaSize() {
        return entry.getParameterAreaSize();
    }

    /**
     * @see ProcedureEntry#getArgumentAreaSize()
     */
    public int getArgumentAreaSize() {
        return entry.getArgumentAreaSize();
    }

    /**
     * @see ProcedureEntry#callsOtherProcedures()
     */
    public boolean requiresArgumentArea() {
        return entry.callsOtherProcedures();
    }

    @Override
    public String toString() {
        String params = this.parameters.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        String decs = this.localDeclarations.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        String scopes = this.body.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));

        return String.format("%s {\n%s\n}", entry.getName(),
                String.join("\n", params, decs, scopes));
    }
}
