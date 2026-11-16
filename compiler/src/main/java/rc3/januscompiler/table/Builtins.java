package rc3.januscompiler.table;

import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;

import java.util.List;

/**
 * This static class provides a collection of utilities and definitions to handle builtin Janus features.
 */
public final class Builtins {
    private Builtins() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("No instances of this class are allowed.");
    }

    /**
     * Creates an instance of an {@link RuntimeException} to represent a not implemented case when dealing
     * with builtins.
     *
     * @param builtin The builtin that was not implemented.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException builtinNotImplemented(Enum<?> builtin) {
        String description = String.format("Builtin '%s.%s' was not implemented!",
                builtin.getDeclaringClass(), builtin.name());
        return new UnsupportedOperationException(description);
    }

    /**
     * An enumeration of procedures builtin into Janus.
     * <p>
     * The definitions provided by this enumeration contain the name of the procedure as well as the parameters
     * required to call the procedure.
     * </p>
     */
    public enum Procedure {
        /**
         * Read an integer from the console.
         */
        READ("read", List.of(PrimitiveType.IntType)),
        /**
         * Write an integer to the console.
         */
        WRITE("write", List.of(PrimitiveType.IntType)),
        /**
         * Read a single character from the console.
         */
        READ_C("readc", List.of(PrimitiveType.IntType)),
        /**
         * Write a single character to the console.
         */
        WRITE_C("writec", List.of(PrimitiveType.IntType)),
        /**
         * Pops a value from a stack into a variable.
         */
        STACK_POP("pop", List.of(PrimitiveType.IntType, PrimitiveType.StackType)),
        /**
         * Pushes the value from a value onto a stack.
         */
        STACK_PUSH("push", List.of(PrimitiveType.IntType, PrimitiveType.StackType));

        private final Identifier name;
        private final List<Type> parameterTypes;

        Procedure(String name, List<Type> parameterTypes) {
            this.name = new Identifier(name);
            this.parameterTypes = parameterTypes;
        }

        public List<Type> getRequiredParameterTypes() {
            return parameterTypes;
        }

        public Identifier getName() {
            return name;
        }
    }

    /**
     * An enumeration of expressions builtin into Janus.
     * <p>
     * The definitions provided by this enumeration contain the name of the expression as well as the value
     * of the expression. A list of types required for parameters is also supplied.
     * </p>
     */
    public enum Expression {
        /**
         * Fetches the top element of a stack.
         */
        TOP("top", PrimitiveType.IntType, List.of(PrimitiveType.StackType)),
        /**
         * Determines whether a stack is empty or not.
         */
        EMPTY("empty", PrimitiveType.BoolType, List.of(PrimitiveType.StackType));

        private final Identifier name;
        private final Type returnType;
        private final List<Type> parameterTypes;

        Expression(String name, Type returnType, List<Type> parameterTypes) {
            this.name = new Identifier(name);
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        public Type getReturnType() {
            return returnType;
        }

        public List<Type> getRequiredParameterTypes() {
            return parameterTypes;
        }

        public Identifier getName() {
            return name;
        }
    }
}
