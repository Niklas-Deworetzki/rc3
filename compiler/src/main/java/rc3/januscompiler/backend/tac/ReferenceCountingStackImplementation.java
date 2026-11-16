package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.AnnotationMode;
import rc3.januscompiler.backend.RuntimeAssertion;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.tac.StackImplementation;
import rc3.januscompiler.tac.TacGenerator;
import rc3.januscompiler.tac.instances.*;

import java.util.function.Supplier;

/**
 * This {@link StackImplementation} realizes stacks as an immutable linked list of stack cells that
 * each hold a single value. The stack cells have a reference counter, so they can be freed if not
 * used anymore.
 */
public record ReferenceCountingStackImplementation(boolean implicitNilChecks) implements StackImplementation {
    private static final Supplier<Constant> predecessorOffset = () -> new Constant("->predecessor");
    private static final Supplier<Constant> valueOffset = () -> new Constant("->value");
    private static final Supplier<Constant> refcountOffset = () -> new Constant("->refcount");

    private void annotate(TacGenerator generator, Instruction instruction, String annotation) {
        if (generator.getAnnotationMode() == AnnotationMode.SOURCE_ANNOTATIONS) {
            instruction.annotate(annotation);
        }
    }

    private void generateLibraryCall(TacGenerator generator, String calledMethod, Operand... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            // Fake a ParamInstruction for every passed argument.
            ParameterType dummyParameterType = new ParameterType(null);
            dummyParameterType.offset = i;

            generator.emit(new ParamInstruction(dummyParameterType, arguments[i]));
        }
        generator.emit(new CallInstruction(calledMethod));
    }

    /**
     * Generates an explicit check for <code>nil</code> if requested by the user.
     */
    private void generateNilCheck(TacGenerator generator, Operand stack, RuntimeAssertion assertion) {
        if (!implicitNilChecks) {
            String labelIsNil = generator.failedAssertionLabel(assertion);

            var nil = generateNil(generator);
            generator.emitBranch(stack, nil, IfGotoInstruction.RelativeOperator.EQU, labelIsNil);
        }
    }

    /**
     * Increase the reference counter of the provided stack.
     */
    private void increaseReferenceCounter(TacGenerator generator, Operand stackReference) {
        String nilNoRefcountLabel = generator.newLabel();
        var nil = generateNil(generator);
        generator.emitBranch(stackReference, nil, IfGotoInstruction.RelativeOperator.EQU, nilNoRefcountLabel);
        {
            var referenceCount = generator.emitLoad(stackReference, refcountOffset.get());
            var updatedReferenceCount = generator.emitArithmetic(referenceCount, new ConstantOperand(1), ArithmeticInstruction.Operator.ADD);
            generator.emit(new StoreInstruction(stackReference, refcountOffset.get(), updatedReferenceCount));
        }
        generator.emit(new Label(nilNoRefcountLabel));
    }

    /**
     * Decrease the reference counter of the provided stack.
     *
     * @implNote The generated code loops if required and cleans up all unreferenced stack cells.
     */
    private void decreaseReferenceCount(TacGenerator generator, Operand stackReference) {
        var activeReference = generator.newTempOperand();
        var copyInstruction = new CopyInstruction(activeReference, stackReference);
        annotate(generator, copyInstruction, "<library code: decrease reference count and free>");
        generator.emit(copyInstruction);

        String startOfFreeLabel = generator.newLabel();
        String endOfFreeLabel = generator.newLabel();
        generator.emit(new Label(startOfFreeLabel));
        // Nil can't be free'd up.
        generator.emitBranch(activeReference, generateNil(generator), IfGotoInstruction.RelativeOperator.EQU, endOfFreeLabel);
        {
            // Decrease the reference count.
            var referenceCount = generator.emitLoad(activeReference, refcountOffset.get());
            var updatedReferenceCount = generator.emitArithmetic(referenceCount, new ConstantOperand(1), ArithmeticInstruction.Operator.SUB);
            generator.emit(new StoreInstruction(activeReference, refcountOffset.get(), updatedReferenceCount));

            // Free stack cell if reference count becomes zero.
            generator.emitBranch(updatedReferenceCount, new ConstantOperand(0), IfGotoInstruction.RelativeOperator.NEQ, endOfFreeLabel);
            {
                var referenceToFree = generator.emitCopy(activeReference);

                var predecessorReference = generator.emitLoad(activeReference, predecessorOffset.get());
                generator.emit(new CopyInstruction(activeReference, predecessorReference));
                generateLibraryCall(generator, "__builtin_sfree", referenceToFree);

                // Loop to decrease predecessor count.
                generator.emit(new GotoInstruction(startOfFreeLabel));
            }
        }
        generator.emit(new Label(endOfFreeLabel));
    }


    @Override
    public Operand generateNil(TacGenerator generator) {
        Constant nil = new Constant("nil", 0);
        return new ConstantOperand(nil);
    }

    @Override
    public void generateComparison(TacGenerator generator, Operand stackReference1, Operand stackReference2,
                                   String equalLabel, String notEqualLabel) {
        // Place both stack references in temporary operands.
        var stackOperand1 = generator.newTempOperand();
        generator.emit(new CopyInstruction(stackOperand1, stackReference1));
        var stackOperand2 = generator.newTempOperand();
        generator.emit(new CopyInstruction(stackOperand2, stackReference2));

        Label labelCompareLoopStart = new Label(generator.newLabel());
        annotate(generator, labelCompareLoopStart, "<library code: compare two stacks>");
        generator.emit(labelCompareLoopStart);
        {
            // Test for reference equality.
            generator.emitBranch(stackOperand1, stackOperand2, IfGotoInstruction.RelativeOperator.EQU, equalLabel);

            // Test if one is nil.
            var nil = generateNil(generator);
            generator.emitBranch(stackOperand1, nil, IfGotoInstruction.RelativeOperator.EQU, notEqualLabel);
            generator.emitBranch(stackOperand2, nil, IfGotoInstruction.RelativeOperator.EQU, notEqualLabel);

            // Compare the values.
            var value1 = generator.emitLoad(stackOperand1, valueOffset.get());
            var value2 = generator.emitLoad(stackOperand2, valueOffset.get());
            generator.emitBranch(value1, value2, IfGotoInstruction.RelativeOperator.NEQ, notEqualLabel);

            // Update references and loop.
            generator.emit(new LoadInstruction(stackOperand1, stackOperand1, predecessorOffset.get()));
            generator.emit(new LoadInstruction(stackOperand2, stackOperand2, predecessorOffset.get()));
            generator.emit(new GotoInstruction(labelCompareLoopStart.name));
        }
    }

    @Override
    public void generateIsEmpty(TacGenerator generator, Operand stackReference, String isEmptyLabel, String notEmptyLabel) {
        var nil = generateNil(generator);
        generator.emitBranch(stackReference, nil, IfGotoInstruction.RelativeOperator.EQU, isEmptyLabel);
        generator.emit(new GotoInstruction(notEmptyLabel));
    }

    @Override
    public void generateAssignment(TacGenerator generator, TemporaryOperand targetVariable, Operand stackReference) {
        increaseReferenceCounter(generator, stackReference);
        generator.emit(new StoreInstruction(targetVariable, stackReference));
    }

    @Override
    public Operand generateTop(TacGenerator generator, TemporaryOperand stackReference) {
        generateNilCheck(generator, stackReference, RuntimeAssertion.TOP_EMPTY_STACK);
        return generator.emitLoad(stackReference, valueOffset.get());
    }

    @Override
    public void generatePush(TacGenerator generator, TemporaryOperand stackVariable, TemporaryOperand value) {
        var oldStackReference = generator.emitLoad(stackVariable);

        generateLibraryCall(generator, "__builtin_salloc", stackVariable);

        var newStackReference = generator.emitLoad(stackVariable);
        // Add reference from the variable pointing at the new cell.
        increaseReferenceCounter(generator, newStackReference);
        generator.emit(new StoreInstruction(newStackReference, predecessorOffset.get(), oldStackReference));
        generator.emit(new StoreInstruction(newStackReference, valueOffset.get(), value));
    }

    @Override
    public void generatePop(TacGenerator generator, TemporaryOperand stackVariable, TemporaryOperand variable) {
        var stackReference = generator.emitLoad(stackVariable);

        generateNilCheck(generator, stackReference, RuntimeAssertion.POP_EMPTY_STACK);

        var predecessor = generator.emitLoad(stackReference, predecessorOffset.get());
        generator.emit(new StoreInstruction(stackVariable, predecessor));
        increaseReferenceCounter(generator, predecessor);

        var poppedValue = generator.emitLoad(stackReference, valueOffset.get());
        generator.emit(new StoreInstruction(variable, poppedValue));
        decreaseReferenceCount(generator, stackReference);
    }

    @Override
    public void generateExit(TacGenerator generator, TemporaryOperand variable) {
        var stackReference = generator.emitLoad(variable);
        decreaseReferenceCount(generator, stackReference);
    }
}
