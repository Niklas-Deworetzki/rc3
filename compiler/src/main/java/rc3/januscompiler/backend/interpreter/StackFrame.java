package rc3.januscompiler.backend.interpreter;

import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.syntax.Tree;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.table.VariableEntry;
import rc3.lib.parsing.Position;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * This class represents an activation frame on the runtime stack of an interpreted Janus program.
 * <p>
 * {@link StackFrame} instances store the parameters and local variables of an invoked {@link ProcedureDeclaration}
 * in an {@link Store} array of the required size.
 * </p>
 * <p>
 * Additionally the caller and the {@link Interpreter.Executor} that
 * created this {@link StackFrame} is required to restore the correct stack trace upon a {@link FailedAssertion}.
 * </p>
 */
public final class StackFrame {
    public final Interpreter.Executor executor;
    public final Position callerPosition;
    public final ProcedureDeclaration procedure;

    private final Store[] localVariables;

    public StackFrame(Interpreter.Executor executor, Tree caller, ProcedureDeclaration procedure) {
        this.executor = executor;
        this.callerPosition = caller.position;
        this.procedure = procedure;

        this.localVariables = new Store[procedure.entry.parameterAreaSize + procedure.entry.localVariableAreaSize];
    }

    /**
     * Allows the caller to place an parameter into this {@link StackFrame}. The offset for this
     * parameter is fetched from the {@link ParameterType}.
     *
     * @param parameterType The {@link ParameterType} holding offset information for the parameter.
     * @param store         The variable that is passed as an parameter.
     */
    public void placeArgument(ParameterType parameterType, Store store) {
        localVariables[parameterType.offset] = store;
    }

    /**
     * Introduces a local variable into the {@link StackFrame}. A {@link Store} for the variable is prepared and
     * placed on the offset fetched from the given {@link VariableEntry}.
     *
     * @param entry The {@link VariableEntry} of the variable introduced to the {@link StackFrame}. It holds offset
     *              and type of the variable.
     * @return The newly created {@link Store} that was prepared on the {@link StackFrame} and is now ready to
     * be used by the introduced variable.
     */
    public Store enter(VariableEntry entry) {
        Store store = Store.forType(entry.type);
        localVariables[entry.offset] = store;
        return store;
    }

    /**
     * Removes a local variable from the {@link StackFrame}. All references to the variables {@link Store} in the
     * {@link StackFrame} are deleted so the {@link Store} can be garbage collected soon.
     *
     * @param entry The {@link VariableEntry} of the variable to be removed from the {@link StackFrame}. It holds the
     *              offset information required to remove the correct {@link Store}.
     */
    public void exit(VariableEntry entry) {
        localVariables[entry.offset] = null;
    }

    /**
     * Fetches the {@link Store} for the given variable.
     *
     * @param entry The {@link VariableEntry} of the variable for which the {@link Store} should be fetched.
     *              It holds the offset information required to fetch the correct {@link Store}.
     * @return The {@link Store} for the given variable.
     */
    public Store get(VariableEntry entry) {
        return localVariables[entry.offset];
    }


    /**
     * Walks the runtime stack of a Janus program to create {@link StackTraceElement} instances.
     * These objects can be used to create a custom stack trace for this exception.
     */
    public static StackTraceElement[] buildStackTrace(Position currentPosition, Deque<StackFrame> runtimeStack) {
        List<StackTraceElement> stackTrace = new ArrayList<>(runtimeStack.size());

        Position tracePosition = currentPosition;
        for (StackFrame executingStackFrame : runtimeStack) {
            StackTraceElement stackTraceElement = createStackTraceElement(executingStackFrame, tracePosition);
            stackTrace.add(stackTraceElement);

            tracePosition = executingStackFrame.callerPosition;
        }

        return stackTrace.toArray(StackTraceElement[]::new);
    }

    private static StackTraceElement createStackTraceElement(StackFrame parentFrame, Position currentPosition) {
        String procedureName = parentFrame.procedure.name.toString();
        String fileName = currentPosition.getSource().getName();

        return new StackTraceElement(parentFrame.executor.getExecutionDirection().toString(),
                procedureName, fileName, currentPosition.getLine());
    }
}
