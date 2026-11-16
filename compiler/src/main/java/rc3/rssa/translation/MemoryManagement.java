package rc3.rssa.translation;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.Constant;
import rc3.rssa.instances.MemoryAccess;

import java.util.Map;

/**
 * This class capsules the memory management required to implement arrays and stacks
 * using RSSA instructions.
 * <p>
 * The first addresses in memory of RSSA are reserved for internal uses such as storing
 * the heap pointer. This value is the address pointing to the lowest unused memory address.
 * </p>
 */
public record MemoryManagement(int reservedStackSize, int totalHeapSize) {

    /**
     * Returns the size of memory allocated for a stack.
     */
    public int getReservedStackSize() {
        return reservedStackSize;
    }

    /**
     * Returns an {@link Annotation} that checks, whether an memory access is valid within the total heap.
     *
     * <code>@InBounds(base=0, size=this.getTotalHeapSize());</code>
     */
    public Annotation inHeapAnnotation() {
        return new Annotation("InBounds", Map.of(
                "base", new Constant(0),
                "size", new Constant(getTotalHeapSize())));
    }

    /**
     * Returns a {@link MemoryAccess} that, when loaded, holds the lowest free address.
     */
    public MemoryAccess getHeapTop() {
        MemoryAccess heapTop = new MemoryAccess(new Constant(getHeapTopAddress()));
        heapTop.annotate(inHeapAnnotation());
        return heapTop;
    }

    /**
     * Returns the address in <var>memory</var> where the heap top is stored.
     */
    public int getHeapTopAddress() {
        return 20;
    }

    /**
     * Returns the amount of memory reserved for internal uses. The value of heap top
     * should be initially set to this value.
     */
    public int getInitialHeapTop() {
        return 100;
    }

    /**
     * Returns the total available heap.
     * <p>
     * This value can be used to implement bound checks upon memory allocation.
     * </p>
     */
    public int getTotalHeapSize() {
        return totalHeapSize;
    }
}
