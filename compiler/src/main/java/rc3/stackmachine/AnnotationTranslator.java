package rc3.stackmachine;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.MemoryView;
import rc3.rssa.instances.Program;
import rc3.stackmachine.syntax.Line;
import rc3.stackmachine.syntax.SetValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class AnnotationTranslator implements Iterable<Line> {
    private final List<Line> generatedLines = new ArrayList<>();

    public AnnotationTranslator(Program program) {
        for (Annotation annotation : program.globalAnnotations()) {
            if (annotation.hasIdentifier("Memory")) {
                setupHeap(annotation.getView());
            }
        }
    }

    private void setupHeap(MemoryView view) {
        generatedLines.add(new SetValue(
                view.getAdress().getValue(),
                view.getValue().getValue()));
    }

    @Override
    public Iterator<Line> iterator() {
        return generatedLines.iterator();
    }
}
