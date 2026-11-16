package rc3.rssa.translation;

import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.backend.RuntimeAssertion;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.tac.ProcedureInvocation;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.instances.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This {@link NameResolver} implementation creates unique names for variables within the same scope and
 * tries to disambiguate user defined variables in contrast to compiler generates ones.
 */
public class RSSANameResolver implements NameResolver {
    private final Map<VariableEntry, String> knownNames = new HashMap<>();

    private String activeProcedure = "";

    @Override
    public String getProcedureName(ProcedureInvocation invocation) {
        return findFreeName(invocation.procedure().getStringName());
    }

    @Override
    public String builtinProcedureName(Builtins.Procedure builtinProcedure) {
        throw new ErrorMessage.InternalError("RSSA does not translate builtin procedures as calls.");
    }

    @Override
    public String failedAssertionLabel(RuntimeAssertion assertion) {
        throw new ErrorMessage.InternalError("RSSA does not use labels for runtime assertions.");
    }

    @Override
    public String getVariableNameInScope(VariableEntry entry) {
        return knownNames.computeIfAbsent(entry,
                (key) -> findFreeName(key.getStringName()));
    }

    public String findFreeName(Identifier proposedName) {
        return findFreeName(proposedName.toString());
    }

    public String findFreeName(String proposedName) {
        return findFreeName(Collections.emptySet(), proposedName);
    }

    public String findFreeName(Collection<String> namesInUse, String proposedName) {
        if (proposedName.matches("T[a-z]+[0-9]+")) { // Disambiguate user-defined names and temporary names.
            proposedName = "var" + proposedName;
        }

        int cycle = 0;
        String name = proposedName;

        while (namesInUse.contains(name)) {
            name = String.format("%s_%d", proposedName, cycle++);
        }

        return name;
    }

    @Override
    public void initializeScope(ProcedureInvocation activeProcedure) {
        this.knownNames.clear();
        this.activeProcedure = getProcedureName(activeProcedure);
    }

    @Override
    public String getTemporaryName(int temporaryNumber) {
        return Variable.nextTemporaryVariable().getRawName();
    }

    @Override
    public String getLabelName(int labelNumber) {
        return String.format("L%s_%d", activeProcedure, labelNumber);
    }

}
