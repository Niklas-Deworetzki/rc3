package rc3.rssa.cse;

import rc3.januscompiler.syntax.AssignStatement;
import rc3.rssa.instances.BinaryOperand;
import rc3.rssa.instances.MemoryInterchangeInstruction;
import rc3.rssa.instances.MemorySwapInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This node represents a wide range of different calculating or memory changing instructions.
 * Its implementation is based on "Compilerbau, Tl. 2" by Alfred V. Aho, Ravi Sethi and Jeffrey D. Ullman (1988), p.668ff.
 * with the addition of additional operands.
 * <p>
 * Each of this nodes contains in total 5 values that are used to identify it and its structural equality:
 * <ul>
 *  <li>A <code>source</code> node, that is destroyed upon evaluation of this node.</li>
 *  <li>A main <code>operator</code> that combines this nodes source with another arithmetic value.</li>
 *  <li>An auxiliary <code>operator</code> that is used to compute this nodes arithmetic value.</li>
 *  <li>Two operands <code>left</code> and <code>right</code> of the auxiliary operator.</li>
 * </ul>
 */
public final class ArithmeticDagNode extends DagNode {
    public final DagNode source;
    public final Operator operator;
    public final DagNode left;
    public final BinaryOperand.BinaryOperator auxiliaryOperator;
    public final DagNode right;

    public final List<CSEAnnotationWrapper> annotations = new ArrayList<>(0);

    public ArithmeticDagNode(DagNode source, Operator operator, DagNode left, BinaryOperand.BinaryOperator auxiliaryOperator, DagNode right) {
        this.source = source;
        this.operator = operator;
        this.left = left;
        this.auxiliaryOperator = auxiliaryOperator;
        this.right = right;
    }

    /**
     * Creates an {@link ArithmeticDagNode} representing a reading memory access.
     *
     * @param index      The index of memory, that is accessed and read.
     * @param boundsBase The lower bound of the memory area, in which the read should happen.
     * @param boundsSize The size of the memory area, in which the read should happen.
     */
    public static ArithmeticDagNode memoryAccess(DagNode index, DagNode boundsBase, DagNode boundsSize) {
        return new ArithmeticDagNode(index, Operator.MEM_READ, boundsBase, null, boundsSize);
    }

    /**
     * Creates an {@link ArithmeticDagNode} representing a {@link MemoryInterchangeInstruction}.
     * The target of the instruction is not passed to this constructor but rather added as a mark to the resulting node.
     *
     * @param destroyed    The source of the memory interchange.
     * @param memoryAccess The memory access that is written to.
     */
    public static ArithmeticDagNode memoryInterchange(DagNode destroyed, DagNode memoryAccess) {
        return new ArithmeticDagNode(destroyed, Operator.MEM_INTERCHANGE, memoryAccess, null, null);
    }

    /**
     * Creates an {@link ArithmeticDagNode} representing a {@link MemorySwapInstruction}.
     *
     * @param memoryLeft  The left memory region that is swapped.
     * @param memoryRight The right memory region that is swapped.
     */
    public static ArithmeticDagNode memorySwap(DagNode memoryLeft, DagNode memoryRight) {
        return new ArithmeticDagNode(null, Operator.MEM_SWAP, memoryLeft, null, memoryRight);
    }


    @Override
    public boolean structurallyEquals(DagNode node) {
        if (!(node instanceof ArithmeticDagNode other)) {
            return false;
        }
        return this.operator == other.operator &&
                this.auxiliaryOperator == other.auxiliaryOperator &&
                Objects.equals(this.source, other.source) &&
                equalOperands(other);
    }

    private boolean equalOperands(ArithmeticDagNode other) {
        if (operator.requiresAuxiliaryOperator && auxiliaryOperator.isCommutative()) {
            // Commutative operators may have operands swapped.
            return (Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right)) ||
                    (Objects.equals(this.left, other.right) && Objects.equals(this.right, other.left));
        }
        return Objects.equals(this.left, other.left) &&
                Objects.equals(this.right, other.right);
    }

    @Override
    public List<DagNode> getDirectChildren() {
        List<DagNode> result = new ArrayList<>(3);
        if (source != null) {
            result.add(source);
        }
        if (left != null) {
            result.add(left);
        }
        if (right != null) {
            result.add(right);
        }
        return result;
    }

    @Override
    public String getSimpleRepresentation() {
        if (operator.requiresAuxiliaryOperator) {
            return String.format(operator.representation, auxiliaryOperator);
        }
        return operator.toString();
    }


    @Override
    public String toString() {
        return String.format("%s marked %s", getSimpleRepresentation(), marks.stream()
                .map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
    }

    public enum Operator {
        /**
         * source + left · right
         */
        ADD(":= _ + (_ %s _)", true, true),
        /**
         * source - left · right
         */
        SUB(":= _ - (_ %s _)", true, true),
        /**
         * source ^ left · right
         */
        XOR(":= _ ^ (_ %s _)", true, true),
        /**
         * M[index] @Bounds(memoryBase, memorySize)
         */
        MEM_READ("M[_]"),
        /**
         * Source[] += left · right
         */
        MEM_ADD("M[_] += _ %s _", true),
        /**
         * Source[] -= left · right
         */
        MEM_SUB("M[_] -= _ %s _", true),
        /**
         * Source[] ^= left · right
         */
        MEM_XOR("M[_] ^= _ %s _", true),

        /**
         * Left[] <=> Right[]
         */
        MEM_SWAP("M[_] <=> M[_]"),

        /**
         * := Left[] := Source
         */
        MEM_INTERCHANGE(":= M[_] := _", false, true);

        private final String representation;
        private final boolean requiresAuxiliaryOperator;
        private final boolean isDestructive;

        Operator(String representation) {
            this(representation, false);
        }

        Operator(String representation, boolean requiresAuxiliaryOperator) {
            this(representation, requiresAuxiliaryOperator, false);
        }

        Operator(String representation, boolean requiresAuxiliaryOperator, boolean isDestructive) {
            this.representation = representation;
            this.requiresAuxiliaryOperator = requiresAuxiliaryOperator;
            this.isDestructive = isDestructive;
        }

        public boolean isDestructive() {
            return isDestructive;
        }

        @Override
        public String toString() {
            return representation;
        }

        public AssignStatement.ModificationOperator toModificationOperator() {
            return switch (this) {
                case ADD, MEM_ADD -> AssignStatement.ModificationOperator.ADD;
                case SUB, MEM_SUB -> AssignStatement.ModificationOperator.SUB;
                case XOR, MEM_XOR -> AssignStatement.ModificationOperator.XOR;
                default -> throw new NoSuchElementException("No ModificationOperator available for " + this);
            };
        }

        public static Operator fromModificationOperator(AssignStatement.ModificationOperator operator, boolean memoryVariant) {
            return switch (operator) {
                case ADD -> memoryVariant ? MEM_ADD : ADD;
                case SUB -> memoryVariant ? MEM_SUB : SUB;
                case XOR -> memoryVariant ? MEM_XOR : XOR;
            };
        }
    }
}
