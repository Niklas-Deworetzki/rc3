package rc3.rssa.translation;

import rc3.januscompiler.syntax.MainProcedureDeclaration;
import rc3.januscompiler.syntax.VariableDeclaration;
import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.Variable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is used to statically allocate the {@link VariableDeclaration declared variables} within
 * the {@link MainProcedureDeclaration main procedure} of a Janus program.
 * <p>
 * An instance of this class will use a {@link MemoryManagement} to reserve heap space for all heap
 * variables and create {@link Annotation Annotations} for them and the heap.
 * <p>
 * {@link VariableDeclaration VariableDeclarations} can be passed together with the
 * {@link Variable Variables} allocated for them to the
 * {@link HeapSpaceAllocator#annotateMainParameters(List, Variable[])} method.
 * This will let this instance annotate the variables and keep track of how many heap
 * space should be reserved for them.
 * <p>
 * After all {@link VariableDeclaration VariableDeclarations} have been processed this way,
 * the {@link HeapSpaceAllocator#initializeHeapWithAnnotations(List)} method can be used to
 * initialize the <i>global annotation list</i> with the {@link Annotation Annotations} required
 * to correctly set up the heap.
 */
public final class HeapSpaceAllocator {
    private final MemoryManagement memoryManagement;
    private int usedHeapSpace;

    public HeapSpaceAllocator(MemoryManagement memoryManagement) {
        this.memoryManagement = memoryManagement;
        this.usedHeapSpace = memoryManagement.getInitialHeapTop();
    }

    private Annotation initializeHeapWithSize() {
        return new Annotation("Heap", Map.of(
                "size", memoryManagement.getTotalHeapSize()
        ));
    }

    private Annotation initializeHeapPointer() {
        return new Annotation("Memory", Map.of(
                "address", memoryManagement.getHeapTopAddress(),
                "value", usedHeapSpace
        ));
    }

    /**
     * Appends required {@link Annotation Annotations} to the <i>global annotation list</i> to
     * correctly initialize the heap.
     * <p>
     * This method will create two {@link Annotation Annotations}:
     * <ol>
     *  <li>A <code>@Heap</code> annotation, setting the heap size as defined in the {@link MemoryManagement}.</li>
     *  <li>A <code>@Memory</code> annotation, setting the heap pointer for a Janus program
     *  to allocate reserved space as well as enough heap space for all declared variables.</li>
     * </ol>
     */
    public void initializeHeapWithAnnotations(List<Annotation> globalAnnotations) {
        globalAnnotations.add(initializeHeapWithSize());
        globalAnnotations.add(initializeHeapPointer());
    }

    private void allocateVariable(VariableDeclaration variableDeclaration, Iterator<Variable> generatedVariables) {
        Variable primaryVariable = generatedVariables.next();
        // FIXME: Annotation field "type" will probably become Enum.

        if (variableDeclaration.type == PrimitiveType.IntType) {
            primaryVariable.annotate(new Annotation("Param", Map.of(
                    "type", "Int",
                    "value", 0,
                    "name", variableDeclaration.name.toString()
            )));

        } else if (variableDeclaration.type == PrimitiveType.StackType) {
            Variable stackTopVariable = generatedVariables.next();
            primaryVariable.annotate(new Annotation("Param", Map.of(
                    "type", "Stack",
                    "value", usedHeapSpace,
                    "name", variableDeclaration.name.toString(),
                    "size", stackTopVariable.copy()
            )));

            stackTopVariable.annotate(new Annotation("Param", Map.of(
                    "type", "Int",
                    "value", 0,
                    "hidden", true
            )));

            usedHeapSpace += memoryManagement.getReservedStackSize();
        } else {
            ArrayType variableType = ((ArrayType) variableDeclaration.type);
            primaryVariable.annotate(new Annotation("Param", Map.of(
                    "type", "Array",
                    "value", usedHeapSpace,
                    "name", variableDeclaration.name.toString(),
                    "size", variableType.size
            )));

            usedHeapSpace += variableType.size;
        }
    }

    /**
     * Allocates all {@link VariableDeclaration declared variables} on the heap and adds
     * <code>@Param</code> annotations for the associated {@link Variable variables}.
     */
    public void annotateMainParameters(List<VariableDeclaration> declaredVariables,
                                       Variable[] generatedVariables) {
        final Iterator<Variable> variableIterator = Arrays.asList(generatedVariables).iterator();

        for (VariableDeclaration declaredVariable : declaredVariables) {
            allocateVariable(declaredVariable, variableIterator);
        }
    }
}
