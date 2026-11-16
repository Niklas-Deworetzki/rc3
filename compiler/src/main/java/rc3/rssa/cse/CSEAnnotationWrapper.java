package rc3.rssa.cse;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.RValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class are used to manage insertion and extraction of {@link Annotation}
 * into and from a {@link CSEGraph}.
 * <p>
 * Creating a new instance of this class will insert a given {@link Annotation} into the
 * given {@link CSEGraph}. A complimentary method is available, which extracts the
 * {@link Annotation} again: {@link CSEAnnotationWrapper#generateAnnotation(CSECodeGenerator)}.
 */
public final class CSEAnnotationWrapper {
    private final String identifier;
    private final Map<String, DagNode> internalizedEntries = new HashMap<>();
    private final Map<String, Object> otherEntries = new HashMap<>();

    public CSEAnnotationWrapper(CSEGraph graph, Annotation annotation) {
        this.identifier = annotation.getIdentifier();

        for (Map.Entry<String, Object> entry : annotation.getData().entrySet()) {
            if (entry.getValue() instanceof RValue) {
                DagNode node = graph.findValueNodeOrInsert((RValue) entry.getValue());
                addEntry(entry.getKey(), node);
            } else {
                addEntry(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addEntry(String key, Object value) {
        otherEntries.put(key, value);
    }

    private void addEntry(String key, DagNode value) {
        internalizedEntries.put(key, value);
    }

    public Annotation generateAnnotation(CSECodeGenerator generator) {
        // Copy regular entries using copy constructor of map.
        Map<String, Object> annotationData = new HashMap<>(otherEntries);

        // Remaining entries have to be extracted from CSE graph.
        for (Map.Entry<String, DagNode> entry : internalizedEntries.entrySet()) {
            RValue value = generator.emitIfRequired(entry.getValue());
            annotationData.put(entry.getKey(), value);
        }

        return new Annotation(identifier, annotationData);
    }

    @Override
    public String toString() {
        return String.format("CSEAnnotationWrapper@%s holding %d entries (%d in CSE)",
                identifier, internalizedEntries.size() + otherEntries.size(), internalizedEntries.size());
    }
}
