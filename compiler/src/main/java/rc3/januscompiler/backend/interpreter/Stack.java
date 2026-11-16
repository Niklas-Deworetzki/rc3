package rc3.januscompiler.backend.interpreter;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class implements a virtual stack instance for Janus stack datatype.
 * <p>
 * Instances of this class are backed by an {@link ArrayDeque} and the {@link Stack#toString()}
 * method is overridden, to represent the stack contents.
 * </p>
 *
 * @param <T> The type of elements stored in this data structure.
 */
public class Stack<T> extends ArrayDeque<T> {

    @Override
    public String toString() {
        if (isEmpty()) {
            return "nil";
        } else {
            return stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "<", "]"));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Stack<?> other)) {
            return false;
        }

        if (size() != other.size()) {
            return false;
        }

        Iterator<T> thisElems = iterator();
        Iterator<?> otherElems = other.iterator();

        while (thisElems.hasNext() && otherElems.hasNext()) {
            if (!Objects.equals(thisElems.next(), otherElems.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toArray());
    }
}
