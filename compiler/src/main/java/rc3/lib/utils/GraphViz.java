package rc3.lib.utils;

public interface GraphViz {

    String graphString();

    String graphId();

    static String validIdFromHash(Object objectToHash) {
        return String.format("h%X", objectToHash.hashCode());
    }

}
