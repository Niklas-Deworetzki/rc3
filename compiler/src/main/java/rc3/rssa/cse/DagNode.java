package rc3.rssa.cse;

import rc3.lib.utils.CollectionUtils;
import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Constant;

import java.util.*;

/**
 * This class represents a node in the directed acyclic graph (DAG) used for common subexpression elimination.
 * <p>
 * Every node must manage a set of all children and parents. While parents are managed in this class for all nodes
 * using the {@link DagNode#registerChildren()} method, children have to be provided by concrete instances.
 * This class provides methods to find all transitive parents and children of a node. Parents are managed by
 * <p>
 * Additionally to <b>direct</b> children, a node can have <b>sequential</b> children, a set of other nodes that
 * it is dependent on but that are not structurally part of a node. These nodes can be added using the
 * {@link DagNode#addSequentialChildren(DagNode...)} method.
 * <p>
 * Every node has a set of marks (which are represented as {@link Atom Atoms}). These marks define the values
 * produces by a node. This base class provides functionality to add and inspect marks for a given node.
 * <p>
 * Equality of nodes is defined in two ways:
 * <ul>
 *  <li>Structural equality</li>
 *  <li>Referential equality</li>
 * </ul>
 * To ensure referential equality and provide a concrete mechanism for doing so, every node holds an unique
 * {@link DagNode#nodeID} that is used to determine equality between two nodes. Since structural equality is
 * also relevant when building the DAG, an additional method {@link DagNode#structurallyEquals(DagNode)} has to
 * be implemented by all concrete subclasses.
 */
public abstract class DagNode {
    protected final Set<Atom> marks = new HashSet<>(0);
    protected final Set<DagNode> sequentialChildren = new HashSet<>();
    protected final Set<DagNode> parents = new HashSet<>();

    private static int sequentialNodeID = 0;
    protected final int nodeID = ++sequentialNodeID;

    /**
     * Adds the given {@link Atom} to the marks of this node.
     */
    public void mark(Atom mark) {
        marks.add(mark);
    }

    /**
     * Returns all marks associated to this node.
     */
    public final Set<Atom> getMarks() {
        return marks;
    }

    /**
     * Returns <code>true</code> if one or more marks are associated with this node.
     */
    public boolean hasMarks() {
        return !marks.isEmpty();
    }

    /**
     * Returns one of the marks associated with this node.
     *
     * @throws NoSuchElementException if no marks are present.
     */
    public final Atom getBestMark() throws NoSuchElementException {
        if (marks.isEmpty()) {
            throw new NoSuchElementException("No marks present!");
        }

        for (Atom mark : marks) {
            if (mark instanceof Constant) {
                return mark;
            }
        }
        return CollectionUtils.anyElement(marks);
    }


    /**
     * Adds the given nodes as sequential children to this node. If one of the nodes passed to this method is
     * also a direct child of this node, it is not added to the sequential children.
     *
     * @throws IllegalStateException If one of the sequential children is the node itself.
     * @throws NullPointerException  If one of the sequential children is null.
     */
    public final void addSequentialChildren(DagNode... sequentialChildren) throws IllegalStateException, NullPointerException {
        addSequentialChildren(Arrays.asList(sequentialChildren));
    }

    /**
     * Adds the given nodes as sequential children to this node. If one of the nodes passed to this method is
     * also a direct child of this node, it is not added to the sequential children.
     *
     * @throws IllegalStateException If one of the sequential children is the node itself.
     * @throws NullPointerException  If one of the sequential children is null.
     */
    public final void addSequentialChildren(Collection<DagNode> sequentialChildren) throws IllegalStateException, NullPointerException {
        Collection<DagNode> directChildren = getDirectChildren();

        for (DagNode sequentialChild : sequentialChildren) {
            if (sequentialChild == null) {
                throw new NullPointerException("Sequential children can't be null!");
            }
            if (sequentialChild == this) {
                throw new IllegalStateException("Sequential children can't create self-loops!");
            }

            if (!directChildren.contains(sequentialChild)) {
                this.sequentialChildren.add(sequentialChild);
            }
        }
    }

    /**
     * Returns all sequential children.
     */
    public final Collection<DagNode> getSequentialChildren() {
        return sequentialChildren;
    }


    /**
     * Returns all direct children.
     */
    public abstract Collection<DagNode> getDirectChildren();

    /**
     * Transitively finds all children of this node.
     */
    public Collection<DagNode> getAllChildren() {
        return CollectionUtils.traverseGraph(HashSet::new,
                DagNode::getDirectChildren, false, this);
    }

    /**
     * Register all direct children of this node. This way the children know, this node is a parent of them.
     */
    public final void registerChildren() {
        getDirectChildren().forEach(this::adopt);
    }

    /**
     * Tell this node to adopt another node. This registers this node as a parent for the other node.
     */
    public final void adopt(DagNode node) {
        node.parents.add(this);
    }

    /**
     * Checks whether this node is a leaf in the DAG. A leaf does not have any children.
     */
    public final boolean isLeaf() {
        return getDirectChildren().isEmpty();
    }

    /**
     * Returns all direct parents.
     */
    public final Collection<DagNode> getDirectParents() {
        return parents;
    }

    /**
     * Transitively finds all parents of this node.
     */
    public final Collection<DagNode> getAllParents() {
        return CollectionUtils.traverseGraph(HashSet::new,
                DagNode::getDirectParents, false, this);
    }

    /**
     * Checks whether this node is a root in the DAG. A root does not have any parents.
     */
    public final boolean isRoot() {
        return getDirectParents().isEmpty();
    }


    /**
     * Returns a simple representation of this node. This representation can be used for debugging or visualization.
     */
    public abstract String getSimpleRepresentation();

    /**
     * Returns the unique id for this node.
     */
    public final int getNodeID() {
        return nodeID;
    }

    @Override
    public final int hashCode() {
        return getNodeID();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof DagNode &&
                ((DagNode) obj).getNodeID() == this.getNodeID();
    }

    /**
     * Checks whether this node is structurally equal to another given node.
     */
    public abstract boolean structurallyEquals(DagNode node);
}
