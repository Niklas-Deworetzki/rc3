package rc3.januscompiler.backend;

import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.tac.ProcedureInvocation;

public interface NameResolver {

    String getProcedureName(ProcedureInvocation invocation);

    String builtinProcedureName(Builtins.Procedure builtinProcedure);

    String failedAssertionLabel(RuntimeAssertion assertion);

    String getVariableNameInScope(VariableEntry entry);

    void initializeScope(ProcedureInvocation activeProcedure);

    default void initializeScope(ProcedureEntry entry) {
        initializeScope(new ProcedureInvocation(entry, null));
    }

    String getTemporaryName(int temporaryNumber);

    String getLabelName(int labelNumber);

}
