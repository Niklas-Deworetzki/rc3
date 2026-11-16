package rc3.rssa.annotations.views;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;
import rc3.rssa.instances.Constant;


/**
 * A view for a memory annotation of the form @Memory(adress=
 */
public class MemoryView extends AnnotationView {

    public MemoryView(Annotation annotation) {
        super(annotation);
    }

    public Constant getAdress() {
        return get("address");
    }

    public Constant getValue() {
        return get("value");
    }
}
