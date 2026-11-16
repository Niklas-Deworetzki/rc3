package rc3.rssa.cp;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;

import java.util.*;

import static rc3.rssa.instances.Instruction.reverseInstructions;

/**
 * This optimization implements local constant propagation. If a {@link Variable} is
 * known to be initialized with the value of another {@link Atom}, it is replaced
 * with this {@link Atom}'s value, eliminating the initializer.
 * {@link Atom} values for a {@link Variable} are propagated within the bounds of a
 * {@link BasicBlock}.
 * <p>
 * This optimization reduces the amount of variables defined in a {@link BasicBlock}
 * and performs static computations.
 */
public final class LocalConstantPropagation extends Program.ProcedureOptimization {

    public LocalConstantPropagation(Compiler compiler) {
        super(AvailableOptimization.LOCAL_CONSTANT_PROPAGATION, compiler);
    }

    /**
     * Performs constant propagation in forward and backward direction.
     */
    private List<Instruction> propagateInBothDirections(List<Instruction> instructions) {
        return reverseInstructions(propagateConstants(reverseInstructions(propagateConstants(instructions))));
    }

    @Override
    public ControlGraph apply(String s, ControlGraph graph) {
        for (BasicBlock block : graph) {
            block.setInstructions(propagateInBothDirections(block.getInstructions()));
        }
        return graph;
    }

    /**
     * Tries to propagate the assignment of <var>source</var> to <var>target</var>.
     * <p>
     * If <var>target</var> is a {@link Variable}, the <var>source</var> {@link Atom}
     * is propagated by placing them into the mutable {@link Map} reference. In this
     * case, no {@link Instruction} has to be generated.
     * <p>
     * If <var>target</var> is a {@link Constant} and <var>source</var> is the
     * <em>same</em> {@link Constant}, no instruction has to be generated.
     * <p>
     * Otherwise, an {@link ArithmeticAssignment#assignAndDestroy(Atom, Atom) assignment}
     * is generated. It is appended to the mutable {@link List} reference.
     * <p>
     * In all cases, where constant propagation was successful and <em>no</em>
     * assignment was generated, this method returns <code>true</code>.
     */
    private boolean propagateAssignment(Atom target, Atom source,
                                        List<Instruction> generatedInstructionBuffer,
                                        Map<Variable, Atom> knownConstantsBuffer) {
        if (target.equals(source)) { // Both are the same constants. Nothing to do.
            debug("CP: %s == %s, no assignment must be generated.\n", target, source);
            return true;
        } else if (target instanceof Variable variable) {
            knownConstantsBuffer.put(variable, source);
            debug("CP: Replaced variable %s with %s\n", target, source);
            return true;
        } else {
            generatedInstructionBuffer.add(ArithmeticAssignment.assignAndDestroy(target, source));
            return false;
        }
    }

    /**
     * Performs constant propagation on a {@link List} of {@link Instruction} instances (i.e. the
     * contents of a {@link BasicBlock}).
     * <p>
     * Assignments of a single {@link Atom} value to a {@link Variable} within the given
     * {@link Instruction Instructions} is eliminated, replacing occurrences of the
     * created {@link Variable} with the assigned {@link Atom}. Furthermore, assignments
     * with only {@link Constant} operands are eliminated, if they are statically known
     * to not raise an assertion error.
     * <p>
     * In practice, this method will analyze two concrete {@link Instruction} instances
     * to perform constant propagation:
     * <ul>
     *  <li>
     *      {@link ArithmeticAssignment} instances with a {@link Variable} on their left-hand
     *      side are used to propagate the value of the {@link Variable} if the right-hand
     *      side of the assignment can be reduced to a single {@link Atom}. If the left-hand
     *      side of the assignment is a {@link Constant}, the assignment is eliminated, if
     *      the right-hand side can be reduced to an equal-value {@link Constant}.
     *  </li>
     *  <li>
     *      {@link SwapInstruction} instances are used to propagate both of the simultaneous
     *      assignments performed by the instruction. If they assign to a {@link Variable},
     *      the assigned {@link Atom} is propagated. If they assign to a {@link Constant},
     *      the assignment is eliminated, if the assigned to {@link Atom} is an equal-value
     *      {@link Constant}. If only one of the two simultaneous assignments can be eliminated
     *      a {@link ArithmeticAssignment#assignAndDestroy(Atom, Atom) single assignment}
     *      is emitted as a replacement for the non-propagated assignment.
     *  </li>
     * </ul>
     */
    private List<Instruction> propagateConstants(List<Instruction> instructions) {
        final Map<Variable, Atom> knownConstants = new HashMap<>();
        final List<Instruction> result = new ArrayList<>();

        for (Instruction instruction : instructions) {
            knownConstants.forEach(instruction::replaceVariable);

            if (instruction instanceof ArithmeticAssignment arithmetic) {
                final OptionalInt evaluatedConstant = arithmetic.getValue().evaluateConstant();
                if (evaluatedConstant.isEmpty()) { // We need some operands and cannot propagate.
                    result.add(arithmetic);

                } else if (arithmetic.source instanceof Constant constant) { // RHS is completely constant.
                    final int rhsConstant = arithmetic.operator.evaluateConstant(constant.value, evaluatedConstant.getAsInt());
                    propagateAssignment(arithmetic.target, new Constant(rhsConstant), result, knownConstants);

                } else if (evaluatedConstant.getAsInt() == 0) {  // Binary operands do not change value of source.
                    propagateAssignment(arithmetic.target, arithmetic.source, result, knownConstants);

                } else {
                    result.add(arithmetic);
                }

            } else if (instruction instanceof SwapInstruction swap) {
                List<Instruction> localBuffer = new ArrayList<>(2);

                if (propagateAssignment(swap.targetLeft, swap.sourceLeft, localBuffer, knownConstants)
                        | propagateAssignment(swap.targetRight, swap.sourceRight, localBuffer, knownConstants)) {
                    result.addAll(localBuffer);
                } else { // We cannot propagate any constant, re-use the original statement.
                    result.add(swap);
                }

            } else {
                result.add(instruction);
            }
        }

        return result;
    }
}
