package rc3.januscompiler.backend.interpreter;

/**
 * This custom subclass of {@link RuntimeException} is used to report operations that execute undefined behavior in
 * the Janus Interpreter.
 * <p>
 * If an undefined state is reached during execution, it will be caught by assertions, which are checked at runtime.
 * If one of these assertions fail, an instance of this class may be instantiated and thrown to report this failure.
 * </p>
 */
public final class FailedAssertion extends RuntimeException {

    public FailedAssertion(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "Failed assertion: " + getMessage();
    }

}
