package rc3.januscompiler.backend.tac;

import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.tac.instances.Instruction;
import rc3.januscompiler.tac.instances.Label;
import rc3.januscompiler.tac.table.Procedure;
import rc3.januscompiler.tac.table.Variable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a function in the C programming language, which is the target architecture for
 * the {@link TacBackend}. This class is a wrapper around the {@link Procedure} class, providing
 * additional information for the C code generation:
 * <ul>
 * <li>The unique name of this translated function.</li>
 * <li>C types for every variable.</li>
 * <li>The functions signature in C.</li>
 * <li>The list of {@link Instruction} objects, that make up the functions body.</li>
 * </ul>
 */

public class CFunction {
    /**
     * The name of this function. It is unique for all generated {@link CFunction CFunctions} in a Janus program.
     */
    public final String name;
    /**
     * The underlying {@link Procedure} object.
     */
    public final Procedure procedure;
    /**
     * This {@link CFunction CFunctions} list of parameters.
     */
    public final List<CVariable> parameters = new ArrayList<>();
    /**
     * All local variables declared in this {@link CFunction}.
     */
    public final Set<CVariable> localVariables = new HashSet<>();
    /**
     * All temporary variables declared in this {@link CFunction}.
     */
    public final Set<CVariable> temporaryVariables = new HashSet<>();

    /**
     * The list of {@link Instruction Instructions} making up the body of this {@link CFunction}.
     */
    public final List<Instruction> body = new ArrayList<>();

    /**
     * Creates a new {@link CFunction} instance with a unique name and an underlying {@link Procedure} object.
     *
     * @param name      The unique name of this {@link CFunction}.
     * @param procedure The {@link Procedure} that is represented by this {@link CFunction}.
     */
    public CFunction(String name, Procedure procedure) {
        this.name = name;
        this.procedure = procedure;
    }

    /**
     * @see Procedure#isJanusMain()
     */
    public boolean isJanusMain() {
        return procedure.isJanusMain();
    }

    /**
     * @see Procedure#getLocalDeclarations()
     */
    public List<Variable> getLocalDeclarations() {
        return procedure.getLocalDeclarations();
    }

    /**
     * @see Procedure#getEntry()
     */
    public ProcedureEntry getEntry() {
        return procedure.getEntry();
    }

    /**
     * @see Procedure#getLocalVariableAreaSize()
     */
    public int getLocalVariableAreaSize() {
        return procedure.getLocalVariableAreaSize();
    }

    /**
     * @see Procedure#getParameterAreaSize()
     */
    public int getParameterAreaSize() {
        return procedure.getParameterAreaSize();
    }

    /**
     * @see Procedure#getArgumentAreaSize()
     */
    public int getArgumentAreaSize() {
        return procedure.getArgumentAreaSize();
    }

    /**
     * @see Procedure#requiresArgumentArea()
     */
    public boolean requiresArgumentArea() {
        return procedure.requiresArgumentArea();
    }

    /**
     * Helper function used to find a {@link CVariable} by name in any {@link Collection}.
     */
    private Optional<CVariable> findFirstWithName(Collection<CVariable> variables, String name) {
        return variables.stream()
                .filter(variable -> variable.name().equals(name))
                .findAny();
    }

    /**
     * Tries to find the parameter with the given name in the parameter list of this {@link CFunction}.
     *
     * @param name The name of the parameter to find.
     * @return The parameter found or empty.
     */
    public Optional<CVariable> findParameter(String name) {
        return findFirstWithName(parameters, name);
    }

    /**
     * Tries to find a local variable with the given name in the set of
     * all local variables of this {@link CFunction}.
     *
     * @param name The name of the local variable to find.
     * @return The local variable found or empty.
     */
    public Optional<CVariable> findLocalVariable(String name) {
        return findFirstWithName(localVariables, name);
    }

    /**
     * Tries to find a temporary variable with the given name in the set of
     * all temporary variables of this {@link CFunction}.
     *
     * @param name The name of the temporary variable to find.
     * @return The temporary variable found or empty.
     */
    public Optional<CVariable> findTemporaryVariable(String name) {
        return findFirstWithName(temporaryVariables, name);
    }

    /**
     * Tries to find a variable with the given name in this {@link CFunction}. This variable may be a parameter,
     * a local variable or a temporary variable.
     *
     * @param name The name of the variable to find.
     * @return The variable found or empty.
     */
    public Optional<CVariable> findVariable(String name) {
        return findParameter(name).or(() ->
                findLocalVariable(name).or(() ->
                        findTemporaryVariable(name)));
    }

    /**
     * Checks whether a label with a given name is declared within this function.
     * <p>
     * Declared labels may be present as an instance of a {@link Label} in the {@link CFunction#body} of this
     * {@link CFunction}.
     * </p>
     * <p>
     * The C standard requires that labels are only valid jump targets, if they are within the same function
     * as the jump.
     * </p>
     *
     * @param labelname The name of the label to check.
     * @return <code>true</code> if a label with the given name exists within this {@link CFunction}.
     */
    public boolean containsLabel(String labelname) {
        return body.stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> label.name.equals(labelname));
    }

    /**
     * Returns a {@link String} representation of this {@link CFunction CFunctions} parameter list
     * as it would be written in C.
     * <p>
     * The parameter list is a comma separated {@link String} of the parameters declaration {@link String Strings}
     * as obtained by calling {@link CVariable#getDeclarationString()}. If there are no parameters the {@link String}
     * <code>"void"</code> is returned.
     * </p>
     */
    protected String getParameterListString() {
        if (parameters.isEmpty()) {
            return "void";
        } else {
            return parameters.stream()
                    .map(CVariable::getDeclarationString)
                    .collect(Collectors.joining(", "));
        }
    }

    /**
     * Returns the signature of this {@link CFunction} as a {@link String}.
     * <p>
     * This signature {@link String} can be used to create a function prototype (by adding a semicolon character)
     * or the function definition by adding the functions body after the signature.
     * </p>
     *
     * @return The signature of this {@link CFunction} as a {@link String}.
     */
    public String getSignatureString() {
        return String.format("void %s(%s)", name, getParameterListString());
    }

    @Override
    public String toString() {
        String parameterList = parameters.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String variables = localVariables.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String instructions = body.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));

        return String.format("CFunction %s (%s)\n{%s} + %d Temporary Variables\n%s", name, parameterList,
                variables, temporaryVariables.size(), instructions);
    }
}
