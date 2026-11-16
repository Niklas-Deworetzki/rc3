package rc3.rssa.annotations.views;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.AnnotationView;
import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Constant;

import java.util.Optional;

/**
 * {@literal @Param }
 * <ul>
 *     <li>type  : String (Array, Stack, Int)</li>
 *     <li>value : Atom</li>
 *     <li>name  : String</li>
 *     <li>hidden: Constant</li>
 * </ul>
 */
public class ParamView extends AnnotationView {

    public ParamView(Annotation annotation) {
        super(annotation);
    }

    public Type getType() {
        return getOrDefault("type", () -> Type.INT);
    }

    public Constant getValue() {
        return getOrDefault("value", () -> new Constant(0));
    }

    public Optional<String> getName() {
        return Optional.ofNullable(get("name"));
    }

    public boolean isHidden() {
        return getOrDefault("hidden", () -> false);
    }

    public Optional<Atom> getSize() {
        return Optional.ofNullable(get("size"));
    }


    public enum Type {
        INT("Int"), ARRAY("Array"), STACK("Stack"), STRING("String");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
