package rc3.rssa.annotations.views;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;
import rc3.rssa.instances.Atom;

/*
 @InBounds(base=ATOM, size=ATOM)
 */
public class InBoundsView extends AnnotationView {

   public InBoundsView(Annotation annotation) {
        super(annotation);
    }

    public Atom getBaseAddress() {
        return get("base");
    }

    public Atom getSize() {
        return get("size");
    }

/*    public static void main(String[] args) {
        Anno anno = null;
        InBoundsView bounds = anno.getView();

        Atom atom = bounds.getBase();
    }*/
}


