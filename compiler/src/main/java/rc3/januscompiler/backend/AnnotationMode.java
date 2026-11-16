package rc3.januscompiler.backend;

/**
 * This enum holds possible modes for annotations generated additional to the emitted code by a backend.
 */
public enum AnnotationMode {
    /**
     * Adds the line number together with the original source code line as annotations.
     */
    SOURCE_ANNOTATIONS,
    /**
     * Ads the line number of the original source code line as annotation.
     */
    LINE_ANNOTATIONS,
    /**
     * Does not add any annotations.
     */
    NO_ANNOTATIONS
}
