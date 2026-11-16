package rc3.stackmachine.syntax;

/**
 * This interface defines all valid {@link Operand} in stackmachine assembler.
 */
public sealed interface Operand {

    /**
     * Create a new {@link Symbol symbolic operand}.
     */
    static Symbol of(String label) {
        return new Symbol(label);
    }

    /**
     * Create a new {@link Constant constant operand}.
     */
    static Constant of(int constant) {
        return new Constant(constant);
    }

    /**
     * Create a new {@link Relative relative operand}.
     */
    static Relative relative(int offset) {
        return new Relative(offset);
    }


    /**
     * A marker interface for {@link Primitive} operands. Only primitive operands are
     * allowed as children of complex operands.
     */
    sealed interface Primitive extends Operand {
    }

    /**
     * Symbolic operands refer to the value of a defined symbol.
     */
    record Symbol(String value) implements Primitive {
        @Override
        public String toString() {
            if (Instruction.isKnownMnemonic(value) || value.charAt(value.length() - 1) == '_') {
                return value + '_'; // Ensure names do not collide with instruction mnemonics.
            }
            return value;
        }
    }

    /**
     * Constant operands have a fixed integer value.
     */
    record Constant(int value) implements Primitive {
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Relative operands add a fixed offset to their position.
     */
    record Relative(int offset) implements Primitive {
        @Override
        public String toString() {
            if (offset > 0) return "@+" + offset;
            else if (offset < 0) return "@" + offset;
            else return "@";
        }
    }


    /**
     * Complex operands perform simple arithmetic operations on their two children.
     */
    sealed abstract class ComplexOperand implements Operand {
        private final Primitive lhs, rhs;

        public ComplexOperand(Primitive lhs, Primitive rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String toString() {
            return String.format("[%s %s %s]", lhs, operatorRepresentation(), rhs);
        }

        public Primitive lhs() {
            return lhs;
        }

        public Primitive rhs() {
            return rhs;
        }

        public abstract int operator(int lhs, int rhs);

        protected abstract String operatorRepresentation();
    }

    /**
     * Perform addition on two simple {@link Operand Operands}.
     */
    final class Add extends ComplexOperand {
        public Add(Primitive lhs, Primitive rhs) {
            super(lhs, rhs);
        }

        @Override
        public int operator(int lhs, int rhs) {
            return lhs + rhs;
        }

        @Override
        protected String operatorRepresentation() {
            return "+";
        }
    }

    /**
     * Perform subtraction on two simple {@link Operand Operands}.
     */
    final class Sub extends ComplexOperand {
        public Sub(Primitive lhs, Primitive rhs) {
            super(lhs, rhs);
        }

        @Override
        public int operator(int lhs, int rhs) {
            return lhs - rhs;
        }

        @Override
        protected String operatorRepresentation() {
            return "-";
        }
    }
}
