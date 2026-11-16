package rc3.rssa.vm;

import java.io.PrintStream;
import java.io.PrintWriter;

public class RSSARuntimeException extends RuntimeException {
    public RSSARuntimeException(String message) {
        super(message);
    }

    public RSSARuntimeException() {
        super();
    }

    @Override
    public String toString() {
        String s =  "Runtime exception occurred";
        return (getMessage() != null) ? (s + ": " + getMessage()) : s;
    }


    @Override
    public void printStackTrace(PrintWriter s) {
        s.println(this);
        StackTraceElement[] trace = super.getStackTrace();
        for (StackTraceElement traceElement : trace)
            s.println("\tat " + formatTraceElement(traceElement));
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        s.println(this);
        StackTraceElement[] trace = super.getStackTrace();
        for (StackTraceElement traceElement : trace)
            s.println("\tat " + formatTraceElement(traceElement));
    }

    private String formatTraceElement(StackTraceElement traceElement) {
        switch (traceElement.getLineNumber()) {
            case -8: // Instruction
                return String.format("instruction\t%s", traceElement.getClassName());
            case -9: // Caller
                return String.format("subroutine\t%s.%s", traceElement.getClassName(), traceElement.getMethodName());
            default:
                return traceElement.toString();
        }
    }
}
