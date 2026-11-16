package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.AbstractCNameResolver;
import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.backend.RuntimeAssertion;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.tac.ProcedureInvocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class translates the names of Janus symbols into valid C names.
 * <p>
 * The name for a {@link ProcedureInvocation} consists of the procedure's name and the suffix <code>"_fw"</code>
 * or <code>"_bw"</code> depending on the execution direction.
 * </p>
 * <p>
 * Variable names get a numeric suffix to create unique names. This {@link NameResolver} also takes the
 * reserved keywords in C into account, so the variable names won't clash with them.
 * </p>
 */
public class CNameResolver extends AbstractCNameResolver {
    protected final Set<String> usedNames = new HashSet<>();
    protected final Map<VariableEntry, String> knownNames = new HashMap<>();
    protected String activeScopePrefix;

    public CNameResolver(Predicate<String> isReservedPreludeName) {
        super(isReservedPreludeName);
    }


    /**
     * Returns a name for the {@link VariableEntry} that has not been used before in the currently active
     * scope and that does not conflict with any C keywords.
     */
    protected String findFreeName(VariableEntry variableEntry) {
        String varName = variableEntry.getStringName();

        // Prevent name clashes with translated procedures.
        if (varName.endsWith("fw") || varName.endsWith("bw")) {
            varName += "var";
        }

        String name = findFreeName(usedNames, varName);
        usedNames.add(name);
        return name;
    }

    @Override
    public String getProcedureName(ProcedureInvocation invocation) {
        return String.format("%s_%s", invocation.procedure().getName(), invocation.direction().asSuffix());
    }

    @Override
    public String builtinProcedureName(Builtins.Procedure builtinProcedure) {
        return String.format("builtin_%s", builtinProcedure.getName());
    }

    @Override
    public String failedAssertionLabel(RuntimeAssertion assertion) {
        return "_ASSERTION_" + assertion.name();
    }

    @Override
    public String getVariableNameInScope(VariableEntry entry) {
        return knownNames.computeIfAbsent(entry, this::findFreeName);
    }

    @Override
    public void initializeScope(ProcedureInvocation activeProcedure) {
        knownNames.clear();
        usedNames.clear();
        activeScopePrefix = getProcedureName(activeProcedure);
    }

    @Override
    public String getTemporaryName(int temporaryNumber) {
        return String.format("_t%d", temporaryNumber);
    }

    @Override
    public String getLabelName(int labelNumber) {
        return String.format("%s_%d", activeScopePrefix, labelNumber);
    }
}
