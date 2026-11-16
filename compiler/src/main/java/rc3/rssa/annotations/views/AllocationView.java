package rc3.rssa.annotations.views;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;
import rc3.rssa.instances.Constant;
import rc3.rssa.instances.RValue;

import java.util.Map;

/*
    @Allocation(usage=M[20], request=256)
 */
public class AllocationView extends AnnotationView implements AnnotationView.ReversibleView {

    public AllocationView(Annotation annotation) {
        super(annotation);
    }

    public RValue heapUsage() {
        return get("usage");
    }

    public Constant requestSize() {
        return get("request");
    }

    @Override
    public Annotation reversed() {
        return new Annotation(getAnnotation().getIdentifier(), Map.of(
                "usage", heapUsage(),
                "request", new Constant(-requestSize().value)));
    }

}
