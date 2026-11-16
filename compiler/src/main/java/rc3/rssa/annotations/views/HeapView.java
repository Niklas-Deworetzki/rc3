package rc3.rssa.annotations.views;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;
import rc3.rssa.instances.Constant;

public class HeapView extends AnnotationView {
    public HeapView(Annotation annotation) {
        super(annotation);
    }

    public Constant getSize() {
        return get("size");
    }
}
