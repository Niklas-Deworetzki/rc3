package rc3.rssa.cse;

import rc3.lib.utils.CollectionUtils;
import rc3.rssa.instances.BinaryOperand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * This node represents entry or exit points of a basic block in the DAG.
 * <p>
 * Additionally to the label and parameter, this entry or exit point refers to, a boolean expression can also
 * be encoded with an additional label for conditional entry or exit points.
 */
public class EntryExitDagNode extends DagNode {
    public final List<DagNode> parameterList = new ArrayList<>();
    public final List<List<CSEAnnotationWrapper>> annotations = new ArrayList<>();
    public final Variant variant;
    public final String primaryLabel;

    public final String secondaryLabel;
    public final DagNode conditionLeft, conditionRight;
    public final BinaryOperand.BinaryOperator comparator;

    public EntryExitDagNode(Variant variant, String label) {
        assert !variant.requiresCondition();

        this.variant = variant;
        this.primaryLabel = label;
        this.secondaryLabel = null;
        this.conditionLeft = null;
        this.conditionRight = null;
        this.comparator = null;
    }

    public EntryExitDagNode(Variant variant, String primaryLabel, String secondaryLabel,
                            DagNode conditionLeft, BinaryOperand.BinaryOperator comparator, DagNode conditionRight) {
        assert variant.requiresCondition();

        this.variant = variant;
        this.primaryLabel = primaryLabel;
        this.secondaryLabel = secondaryLabel;
        this.conditionLeft = conditionLeft;
        this.comparator = comparator;
        this.conditionRight = conditionRight;
    }

    @Override
    public Collection<DagNode> getDirectChildren() {
        List<DagNode> children = new ArrayList<>();
        if (variant.isExit) {
            children.addAll(parameterList);
        }
        if (variant.requiresCondition) {
            children.add(conditionLeft);
            children.add(conditionRight);
        }
        return children;
    }

    @Override
    public String getSimpleRepresentation() {
        if (variant.requiresCondition) {
            return String.format(variant.representation, primaryLabel, secondaryLabel, comparator);
        }
        return String.format(variant.representation, primaryLabel);
    }

    @Override
    public boolean structurallyEquals(DagNode node) {
        if (!(node instanceof EntryExitDagNode other)) {
            return false;
        }
        return this.variant == other.variant &&
                Objects.equals(this.primaryLabel, other.primaryLabel) &&
                Objects.equals(this.secondaryLabel, other.secondaryLabel) &&
                areConditionsEqual(this, other) &&
                CollectionUtils.zipWith(this.parameterList, other.parameterList, DagNode::structurallyEquals)
                        .allMatch(Boolean::booleanValue);
    }

    private static boolean areConditionsEqual(EntryExitDagNode n1, EntryExitDagNode n2) {
        return n1.comparator == n2.comparator &&
                n1.conditionLeft != null && n2.conditionLeft != null &&
                n1.conditionLeft.structurallyEquals(n2.conditionLeft) &&
                n1.conditionRight != null && n2.conditionRight != null &&
                n1.conditionRight.structurallyEquals(n2.conditionRight);
    }


    public enum Variant {
        BEGIN("begin %s", false),
        END("end %s", true),
        ENTRY_UNCONDITIONAL("%s <-", false),
        EXIT_UNCONDITIONAL("%s ->", true),
        ENTRY_CONDITIONAL("%s()%s <- _ %s _", false, true),
        EXIT_CONDITIONAL("%s()%s -> _ %s _", true, true);

        private final String representation;
        private final boolean isExit;
        private final boolean requiresCondition;

        Variant(String representation, boolean isExit) {
            this(representation, isExit, false);
        }

        Variant(String representation, boolean isExit, boolean requiresCondition) {
            this.representation = representation;
            this.isExit = isExit;
            this.requiresCondition = requiresCondition;
        }

        public boolean isExit() {
            return isExit;
        }

        public boolean requiresCondition() {
            return requiresCondition;
        }

        @Override
        public String toString() {
            return representation;
        }
    }
}
