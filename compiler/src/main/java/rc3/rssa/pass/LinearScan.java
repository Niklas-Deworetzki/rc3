package rc3.rssa.pass;

import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.ControlInstruction;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.SwapInstruction;
import rc3.rssa.instances.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A simple {@link StackAllocation.Strategy} that favours execution time over
 * the optimal stack layout.
 */
final class LinearScan implements StackAllocation.Strategy {
    private final ControlGraph procedure;

    private final IntegerHeap heap;
    private final Map<Variable, Integer> offsets = new HashMap<>();
    private int highestOffset = -1;

    public LinearScan(ControlGraph procedure) {
        this.procedure = procedure;

        int upperVariablesBound = procedure.getNodes().stream()
                .mapToInt(LinearScan::countVariables)
                .reduce(0, Math::max);
        this.heap = new IntegerHeap(upperVariablesBound);

        initialize();
    }

    private static int countVariables(BasicBlock block) {
        return block.getInstructions().stream()
                .mapToInt(instruction -> instruction.variablesCreated().size())
                .sum();
    }

    private void initialize() {
        for (BasicBlock block : procedure) {
            for (Instruction instruction : block) {
                if (instruction instanceof SwapInstruction swap &&
                        updateOptimizedSwap(swap))
                    continue; // Optimized path was taken.

                if (instruction instanceof ControlInstruction) {
                    created(instruction);
                    destroyed(instruction);
                } else {
                    destroyed(instruction);
                    created(instruction);
                }
            }
        }
    }

    private boolean updateOptimizedSwap(SwapInstruction swap) {
        if (swap.sourceLeft instanceof Variable sl &&
                swap.sourceRight instanceof Variable sr &&
                swap.targetLeft instanceof Variable tl &&
                swap.targetRight instanceof Variable tr) {
            // If we just assign variables, we can pass on the stack slots.
            offsets.put(tl, offsets.get(sl));
            offsets.put(tr, offsets.get(sr));
            return true;
        }
        return false;
    }

    private void created(Instruction instruction) {
        for (Variable created : instruction.variablesCreated()) {
            int offset = heap.pop();
            offsets.put(created, offset);
            highestOffset = Math.max(highestOffset, offset);
        }
    }

    private void destroyed(Instruction instruction) {
        for (Variable destroyed : instruction.variablesDestroyed()) {
            heap.push(offsets.get(destroyed));
        }
    }

    @Override
    public int requiredStackSpace() {
        return highestOffset + 1;
    }

    @Override
    public int offsetFor(Variable variable) {
        return offsets.get(variable);
    }


    private static final class IntegerHeap {
        private final boolean[] data;
        private int scan = 0;

        public IntegerHeap(int capacity) {
            this.data = new boolean[capacity];
            Arrays.fill(this.data, true);
        }

        private void rangeCheck(int value) throws NoSuchElementException {
            if (value < 0 || value >= data.length) {
                throw new NoSuchElementException("Integer value " + value + " is out of range.");
            }
        }

        public void push(int value) throws NoSuchElementException {
            rangeCheck(value);
            scan = Math.min(scan, value);
            data[value] = true;
        }

        public int pop() throws NoSuchElementException {
            rangeCheck(scan);
            int result = scan;
            data[scan] = false;

            while (scan < data.length && !data[scan]) {
                scan++;
            }

            return result;
        }
    }
}
