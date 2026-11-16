package rc3.lib.utils;

public interface RestrictedOperations {

    default <A> A notImplementedFor(Object instance) {
        String message = String.format("Instances of class %s are not supported in this operation.", instance.getClass().getCanonicalName());
        throw new UnsupportedOperationException(message);
    }

}
