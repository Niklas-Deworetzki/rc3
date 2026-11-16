package rc3.rssa.cse;

import rc3.rssa.instances.Atom;

import java.util.Collections;
import java.util.Set;

/**
 * This node represents {@link Atom} operands in the DAG. It has a single {@link Atom} as its value and
 * no children.
 */
public final class AtomicDagNode extends DagNode {
    private final Atom value;

    public AtomicDagNode(Atom value) {
        this.value = value.copy();
        // Strip annotations of atomic nodes, so we don't have issues with them later.
        this.value.getAnnotations().clear();
    }

    public Atom getValue() {
        return value;
    }

    @Override
    public boolean structurallyEquals(DagNode other) {
        return other instanceof AtomicDagNode &&
                ((AtomicDagNode) other).value.equals(this.value);
    }

    @Override
    public Set<DagNode> getDirectChildren() {
        return Collections.emptySet();
    }

    @Override
    public String getSimpleRepresentation() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
