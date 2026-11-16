package rc3.rssa.vm;

/**
 * This custom subclass of {@link RuntimeException} is used to report operations that execute undefined behavior in
 * the RSSA Interpreter.
 * <p>
 * If an undefined state is reached during execution, it will be caught by assertions, which are checked at runtime.
 * If one of these assertions fail, an instance of this class may be instantiated and thrown to report this failure.
 * </p>
 */
public class FailedAssertion extends RSSARuntimeException {

    public FailedAssertion(String message) {
        super(message);
    }

    public FailedAssertion() {
        super();
    }

    @Override
    public String toString() {
        String s = "Failed assertion";
        return (getMessage() != null) ? (s + ": " + getMessage()) : s;
    }

}
