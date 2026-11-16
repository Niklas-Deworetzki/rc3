package rc3.lib.dataflow;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;

/**
 * A class holding the solution to a {@link DataflowProblem}.
 * <p>
 * Both the <b>IN</b> and <b>OUT</b> values for every node of {@link DataflowProblem}
 * are present on instances of this class.
 */
public final class DataflowSolution<N, L> extends AbstractCollection<N> {
    private final Map<N, L> inValues, outValues;

    public DataflowSolution(Map<N, L> inValues, Map<N, L> outValues) {
        this.inValues = inValues;
        this.outValues = outValues;
        assert inValues.keySet().equals(outValues.keySet()) : "IN and OUT must be defined for the same keys.";
    }

    public Map<N, L> getIn() {
        return inValues;
    }

    public Map<N, L> getOut() {
        return outValues;
    }

    public L getIn(N node) {
        return inValues.get(node);
    }

    public L getOut(N node) {
        return outValues.get(node);
    }

    // Implemented Collection methods.

    @Override
    public Iterator<N> iterator() {
        return inValues.keySet().iterator();
    }

    @Override
    public int size() {
        return inValues.size();
    }

    @Override
    public boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return inValues.containsKey(o);
    }

    @Override
    public boolean add(N n) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
