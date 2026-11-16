package rc3.rssa.annotations.views;

import rc3.lib.parsing.Position;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;

/*
 * @Source(position=...)
 */
public class SourceView extends AnnotationView {

    public SourceView(Annotation annotation) {
        super(annotation);
    }

    public Position getPosition() {
        return get("position");
    }
}
