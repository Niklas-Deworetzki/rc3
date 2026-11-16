package rc3.januscompiler.backend.interpreter;

import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;

import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Collectors;

/**
 * The {@link Store} interface provides instances and methods to <i>store</i> variables and their
 * values during interpretation of a Janus program.
 * <p>
 * The name is inspired by the 2007 paper "A Reversible Programming Language and its Invertible Self-Interpreter"
 * by Tetsuo Yokoyama and Robert Glück.
 * </p>
 * <p>
 * Implementations of this interface should override the {@link Store#toString()} method, that is used to
 * print the values of variables.
 * </p>
 */
public sealed interface Store {

    /**
     * Updates the data in this store.
     */
    void setData(Object value);

    /**
     * Retrieves the data in this store.
     */
    Object getData();

    /**
     * Creates an adequate {@link Store} for the given Janus {@link Type}.
     */
    static Store forType(Type type) {
        if (type == PrimitiveType.IntType) {
            return new IntStore();
        } else if (type == PrimitiveType.StackType) {
            return new StackStore();
        } else if (type instanceof ArrayType) {
            int size = ((ArrayType) type).size;
            return new ArrayStore(size);
        }

        throw new RuntimeException("Unknown type: " + type);
    }

    /**
     * {@link Store} for variables of the primitive int type
     *
     * @see PrimitiveType#IntType
     */
    final class IntStore implements Store {
        public int data;

        public IntStore() {
            this.data = 0;
        }

        @Override
        public void setData(Object value) {
            this.data = (Integer) value;
        }

        @Override
        public Integer getData() {
            return data;
        }

        @Override
        public String toString() {
            return Integer.toString(data);
        }
    }

    /**
     * Store for variables of Janus array types.
     *
     * @see ArrayType
     */
    final class ArrayStore implements Store {
        public final Store[] data;

        public ArrayStore(int size) {
            this.data = new IntStore[size];
            for (int i = 0; i < size; i++) {
                this.data[i] = Store.forType(PrimitiveType.IntType);
            }
        }

        @Override
        public void setData(Object value) {
            ArrayStore other = (ArrayStore) value;
            for (int i = 0; i < data.length; i++) {
                data[i].setData(other.data[i].getData());
            }
        }

        @Override
        public Store[] getData() {
            return data;
        }

        @Override
        public String toString() {
            return Arrays.stream(data)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    /**
     * Store for the primitive stack type.
     *
     * @see PrimitiveType#StackType
     */
    final class StackStore implements Store {
        public final Deque<Integer> data;

        public StackStore() {
            this.data = new Stack<>();
        }

        @Override
        public void setData(Object value) {
            @SuppressWarnings("unchecked") Deque<Integer> dequeValue = (Deque<Integer>) value;
            data.clear();
            dequeValue.forEach(data::addLast);
        }

        @Override
        public Deque<Integer> getData() {
            return data;
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }
}
