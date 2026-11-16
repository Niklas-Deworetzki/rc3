package rc3.rssa.cse;

import rc3.januscompiler.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static rc3.lib.utils.CollectionUtils.zipWith;

/**
 * This node represents a procedure call. It stores direction and name of the called procedure as well as
 * a list of parameters, which are destroyed upon evaluation.
 */
public final class ProcedureDagNode extends DagNode {
    public final List<DagNode> parameters = new ArrayList<>();
    public final List<DagNode> results = new ArrayList<>();
    public final Direction direction;
    public final String procedure;

    /**
     * Creates a new instance of this node given a procedure and its call direction.
     *
     * <b>Note:</b> the list of parameters as well as results is not passed as an constructor argument
     * but rather is added by the DAG constructing this node, since this instance has to be known before
     * parameter and result nodes can be created.
     */
    public ProcedureDagNode(Direction direction, String procedure) {
        this.direction = direction;
        this.procedure = procedure;
    }

    @Override
    public Collection<DagNode> getDirectChildren() {
        return parameters;
    }

    @Override
    public String getSimpleRepresentation() {
        return String.format("%s %s(_)",
                direction.asCallKeyword(),
                procedure);
    }

    @Override
    public boolean structurallyEquals(DagNode node) {
        if (!(node instanceof ProcedureDagNode other)) {
            return false;
        }

        return this.procedure.equals(other.procedure) &&
                this.direction == other.direction &&
                zipWith(this.parameters, other.parameters, DagNode::structurallyEquals)
                        .allMatch(Boolean::booleanValue);
    }
}
