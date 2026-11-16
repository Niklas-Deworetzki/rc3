package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.TacGenerator;
import rc3.januscompiler.tac.visitor.VisitableTac;

/**
 * This abstract class is the root of all three-address-code instructions.
 * <p>
 * It implements the {@link VisitableTac} interface and thus can be used in the visitor pattern.
 * </p>
 * <p>
 * Instructions can be annotated with a {@link String}. This is used in the
 * {@link TacGenerator} to embed references to the source code
 * in generated {@link Instruction Instructions}.
 * </p>
 */
public abstract class Instruction implements VisitableTac {
    private String annotation;

    public boolean hasAnnotation() {
        return annotation != null;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void annotate(String annotation) {
        this.annotation = annotation;
    }
}
