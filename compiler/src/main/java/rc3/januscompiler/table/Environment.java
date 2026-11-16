package rc3.januscompiler.table;

import java.util.*;

/**
 * An {@link Environment} maps all visible names to the {@link Entry} associated with them.
 * <p>
 * This class is optimized for the scope that Janus allows for variables to be defined in.
 * </p>
 * <p>
 * {@link ProcedureEntry ProcedureEntries} are stored in a global {@link Map}, since they can only be
 * defined in a global scope.
 * </p>
 * <p>
 * {@link VariableEntry VariableEntries} are pushed onto a stack-like data structure implemented by
 * the {@link Deque} interface in Java. Searching though this stack from top to bottom represents
 * the scopes introduced by nested local blocks. By returning the first entry with a matching name
 * when looking up variables, the shadowing rules of local blocks are automatically applied.
 * </p>
 * <p>
 * While the global scope can remain constant after the global analysis pass, the scope of local
 * variables has to be managed manually. This is done by calling {@link Environment#enter(VariableEntry)}
 * to introduce local variables and calling {@link Environment#exit(VariableEntry)} to remove them again.
 * During removal an assertion can be made, that the name of the removed variable is equal to the name
 * that should be removed, since the scope management should also happen in a stack-like manner.
 * </p>
 */
public final class Environment {
    private final Map<Identifier, ProcedureEntry> procedures;
    private final Deque<VariableEntry> variables;

    public Environment() {
        this.procedures = new HashMap<>();
        this.variables = new ArrayDeque<>();
    }

    public boolean enterGlobal(ProcedureEntry entry) {
        ProcedureEntry previousEntry = procedures.put(entry.name, entry);
        return previousEntry == null;
    }

    /**
     * Finds the global procedure with the given name.
     *
     * @param name The name of the procedure to find.
     * @return The globally defined {@link ProcedureEntry} with the given name or {@link Optional#empty()}.
     */
    public Optional<ProcedureEntry> getProcedure(Identifier name) {
        return Optional.ofNullable(procedures.get(name));
    }

    /**
     * Finds the variable with the given name in the local scope.
     *
     * @param name The name of the variable to find.
     * @return The locally defined {@link VariableEntry} with the given name of {@link Optional#empty()}.
     */
    public Optional<VariableEntry> getVariable(Identifier name) {
        for (VariableEntry entry : variables) {
            if (entry.name.equals(name)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Introduces a new {@link VariableEntry}. It is now visible to all subsequent calls
     * to {@link Environment#getVariable(Identifier)} until the {@link VariableEntry} is
     * removed from the {@link Environment} again.
     *
     * @param entry The {@link VariableEntry} to introduce.
     */
    public void enter(VariableEntry entry) {
        variables.push(entry);
    }

    /**
     * Removes a {@link VariableEntry} from the local scope. The top entry is removed from
     * the scope stack. The names of the provided and removed {@link VariableEntry} are
     * asserted to be equal.
     *
     * @param entry The last {@link VariableEntry} introduced in the {@link Environment}.
     */
    public void exit(VariableEntry entry) {
        VariableEntry removedEntry = variables.pop();
        assert removedEntry.name.equals(entry.name);
    }

    /**
     * Removes all {@link VariableEntry VariableEntries} from the local scope. No assertions
     * are made.
     */
    public void clearLocalVariables() {
        variables.clear();
    }
}
