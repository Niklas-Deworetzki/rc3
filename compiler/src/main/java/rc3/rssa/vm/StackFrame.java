package rc3.rssa.vm;

import rc3.januscompiler.Direction;
import rc3.lib.parsing.Position;
import rc3.rssa.annotations.Annotatable;
import rc3.rssa.instances.Variable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class represents a rssa stack frame.
 */
public class StackFrame {
    public Direction calleeDirection; // this direction
    public Direction callerDirection; // old direction
    public final int oldIp; // return address
    private final Optional<Position> callerPosition;
    private final String procedure;
    public final String callerLabel;
    private final int[] localVariables;
    // Debug information
    private final String[] variableNames;

    public StackFrame(Direction calleeDirection, Direction callerDirection, int size, int ip,
                      Optional<Position> position, String calleeLabel, String callerLabel) {
        this.callerDirection = callerDirection;
        this.calleeDirection = calleeDirection;
        this.callerPosition = position;
        this.localVariables = new int[size];
        this.variableNames = new String[size];
        this.oldIp = ip;
        this.procedure = calleeLabel;
        this.callerLabel = callerLabel;
    }

    public void enter(Variable variable, int value) {
        localVariables[variable.offset] = value;
        variableNames[variable.offset] = variable.getRawName(); // TODO does not work with compiler, but only for debugger relevant
    }

    public void exit(Variable variable) {
        variableNames[variable.offset] = null;
    }

    public int get(Variable variable) {
        return localVariables[variable.offset];
    }

    public int get(String variableName) throws IllegalAccessException {
        for (int i = 0; i < variableNames.length; i++) {
            if (variableNames[i] != null && variableNames[i].equals(variableName)) {
                return localVariables[i];
            }
        }
        throw new IllegalAccessException(
                String.format("Variable '%s' accessed before definition or after destruction.", variableName));
    }

    public boolean allDestroyed() {
        for (String name : variableNames) {
            if (name != null) {
                return false;
            }
        }
        return true;
    }

    public String[] getVariableNames() {
        return variableNames;
    }

    @Override
    public String toString() {
        return String.format("%-20s %s", calleeDirection + "." + procedure + "()",
                callerPosition.map(position -> "at " + position.getFilenameAndLine()).orElse(""));
    }

    public static StackTraceElement[] buildStackTrace(Optional<Position> currentPosition,
                                                      Deque<StackFrame> runtimeStack) {
        List<StackTraceElement> stackTrace = new ArrayList<>(runtimeStack.size());

        Optional<Position> tracePosition = currentPosition;
        for (StackFrame executingStackFrame : runtimeStack) {
            StackTraceElement stackTraceElement = tracePosition
                    .map(position -> createStackTraceElement(executingStackFrame, position))
                    .orElseGet(() -> createStackTraceElement(executingStackFrame)); // no postition known
            stackTrace.add(stackTraceElement);

            tracePosition = executingStackFrame.callerPosition;
        }

        return stackTrace.toArray(StackTraceElement[]::new);
    }

    private static StackTraceElement createStackTraceElement(StackFrame parentFrame, Position currentPosition) {
        String fileName = currentPosition.getSource().getName();

        return new StackTraceElement(parentFrame.calleeDirection.toString(),
                parentFrame.procedure, fileName, currentPosition.getLine());
    }

    /**
     * This method builds a java stack trace for the current rssa runtime stack if the no current position is known,
     * since source annotations are optional.
     *
     * @param annotated    The instruction or value the error occurred
     * @param runtimeStack The current rssa runtime stack
     * @return an array representing the stack trace
     */
    public static StackTraceElement[] buildStackTrace(Annotatable annotated, Deque<StackFrame> runtimeStack) {
        StackTraceElement instructionStackTraceElement = new StackTraceElement(annotated.toString(), "", null, -8);
        return Stream.of(new StackTraceElement[]{instructionStackTraceElement},
                buildStackTrace(Optional.empty(), runtimeStack)).flatMap(Stream::of).toArray(StackTraceElement[]::new);
    }

    private static StackTraceElement createStackTraceElement(StackFrame parentFrame) {
        return new StackTraceElement(parentFrame.callerDirection.toString(), parentFrame.procedure, null, -9);
    }
}

