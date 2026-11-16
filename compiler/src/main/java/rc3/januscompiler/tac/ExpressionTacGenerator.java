package rc3.januscompiler.tac;

import rc3.januscompiler.backend.RuntimeAssertion;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.VariableEntry;
import rc3.januscompiler.tac.instances.*;
import rc3.januscompiler.types.ArrayType;

import java.util.Set;

/**
 * This lass is used to generate code for {@link Expression} and {@link Variable}
 * instances. Since they usually calculate a <i>value</i>, the code generating method
 * returns an {@link Operand} where this value can be found after execution of the code.
 * <p>
 * {@link Variable Variables} hold an address in their {@link Operand} value. This address points
 * to a location on the stack frame, where the contents of the {@link Variable} are stored.
 * When local variables are parameters, the {@link Operand} will hold the address of the
 * original location of the variable.
 * </p>
 * <p>
 * Expressions are usually evaluated to their result. The exception here are boolean expressions
 * which are translated to control flow instructions as seen in "Compilerbau, Tl. 2" by Alfred V. Aho,
 * Ravi Sethi and Jeffrey D. Ullman (1988), p.605 ff.
 * </p>
 * <p>
 * The control flow translation is achieved by passing two labels to the constructor of this class.
 * These labels define the jump target of the instructions generated.
 * </p>
 */
class ExpressionTacGenerator extends DoNothingTreeVisitor<Operand> {
    protected final TacGenerator generator;
    protected final String trueLabel;
    protected final String falseLabel;

    /**
     * Creates a new {@link ExpressionTacGenerator} instance without jump targets.
     * This constructor is used to create the shared instance used by all top-level expressions.
     */
    public ExpressionTacGenerator(TacGenerator generator) {
        this(generator, null, null);
    }

    /**
     * Creates a new {@link ExpressionTacGenerator} instance with <code>true</code> and
     * <code>false</code> jump targets. Control flow will be redirected to those targets depending
     * on the boolean value of the evaluated expression.
     *
     * @param trueLabel  Where to jump if the expression evaluates to <code>true</code>.
     * @param falseLabel Where to jump if the expression evaluates to <code>false</code>.
     */
    public ExpressionTacGenerator(TacGenerator generator, String trueLabel, String falseLabel) {
        this.generator = generator;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    @Override
    public TemporaryOperand visit(IndexedVariable indexedVariable) {
        VariableEntry variableEntry = generator.lookupVariable(indexedVariable.name);
        ArrayType type = (ArrayType) variableEntry.type;

        // Get address and index.
        var variableAddress = generator.loadVariableAddress(variableEntry);
        var index = generator.generateExpression(indexedVariable.index);

        // Check the array bounds.
        String indexErrorLabel = generator.failedAssertionLabel(RuntimeAssertion.ARRAY_INDEX);
        var arraySize = new ConstantOperand(type.size);
        generator.emitBranch(arraySize, index, IfGotoInstruction.RelativeOperator.U_LSE, indexErrorLabel);

        // Calculate the element's address as: base + i * elementSize
        Constant elementSize = generator.runtime.elementSize(type);
        var scaledIndex = generator.newTempOperand();
        var resultAddress = generator.newTempOperand();

        generator.emit(new ArithmeticInstruction(scaledIndex, index, new ConstantOperand(elementSize), ArithmeticInstruction.Operator.MUL));
        generator.emit(new ArithmeticInstruction(resultAddress, variableAddress, scaledIndex, ArithmeticInstruction.Operator.ADD));
        return resultAddress;
    }

    @Override
    public TemporaryOperand visit(NamedVariable namedVariable) {
        VariableEntry variableEntry = generator.lookupVariable(namedVariable.name);
        return generator.loadVariableAddress(variableEntry);
    }

    @Override
    public Operand visit(BinaryExpression binaryExpression) {
        switch (binaryExpression.operator.getType()) {
            case ARITHMETIC, BITWISE -> { // Arithmetic and bitwise operators are implemented using 3AC assignments.
                var lhs = generator.generateExpression(binaryExpression.lhs);
                var rhs = generator.generateExpression(binaryExpression.rhs);

                // The right-hand side may be zero. This leads to an error when used in division or modulo operation.
                if (generator.runtime.checkArithmetic() &&
                        Set.of(BinaryExpression.BinaryOperator.DIV, BinaryExpression.BinaryOperator.MOD).contains(binaryExpression.operator)) {
                    String divByZeroLabel = generator.failedAssertionLabel(RuntimeAssertion.DIVISION_BY_ZERO);
                    generator.emitBranch(rhs, new ConstantOperand(0), IfGotoInstruction.RelativeOperator.EQU, divByZeroLabel);
                }

                var result = generator.newTempOperand();
                ArithmeticInstruction.Operator operator = ArithmeticInstruction.Operator.getEquivalentOperator(binaryExpression.operator);
                generator.emit(new ArithmeticInstruction(result, lhs, rhs, operator));
                return result;
            }
            case RELATIONAL -> { // Relational expressions are translated into control flow instructions.
                var lhs = generator.generateExpression(binaryExpression.lhs);
                var rhs = generator.generateExpression(binaryExpression.rhs);
                switch (binaryExpression.operator) {
                    case EQU -> generator.emitBranch(lhs, rhs, IfGotoInstruction.RelativeOperator.EQU, trueLabel);
                    case NEQ -> generator.emitBranch(lhs, rhs, IfGotoInstruction.RelativeOperator.NEQ, trueLabel);
                    case LSE -> generator.emitBranch(lhs, rhs, IfGotoInstruction.RelativeOperator.LSE, trueLabel);
                    case LST -> generator.emitBranch(lhs, rhs, IfGotoInstruction.RelativeOperator.LST, trueLabel);


                    // Invert operand order to perform "Greater" operations using "Less".
                    case GRE -> generator.emitBranch(rhs, lhs, IfGotoInstruction.RelativeOperator.LSE, trueLabel);
                    case GRT -> generator.emitBranch(rhs, lhs, IfGotoInstruction.RelativeOperator.LST, trueLabel);
                }
                generator.emit(new GotoInstruction(falseLabel));
                return null;
            }
            case LOGIC -> { // Logical expressions are translated into control flow ("Compilerbau, Tl.2", Fig. 8.24)
                String intermediateLabel = generator.newLabel();
                switch (binaryExpression.operator) {
                    case DISJ -> {
                        generator.generateCondition(binaryExpression.lhs, trueLabel, intermediateLabel);
                        generator.emit(new Label(intermediateLabel));
                        generator.generateCondition(binaryExpression.rhs, trueLabel, falseLabel);
                        return null;
                    }
                    case CONJ -> {
                        generator.generateCondition(binaryExpression.lhs, intermediateLabel, falseLabel);
                        generator.emit(new Label(intermediateLabel));
                        generator.generateCondition(binaryExpression.rhs, trueLabel, falseLabel);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Operand visit(BuiltinExpression builtinExpression) {
        switch (builtinExpression.expression) {
            case EMPTY -> {
                // Get the stack reference.
                var stackVariable = generator.generateVariable(builtinExpression.argumentList.get(0));
                var stackReference = generator.emitLoad(stackVariable);

                // Let the runtime translate boolean expression to control flow.
                generator.stackImplementation.generateIsEmpty(generator, stackReference, trueLabel, falseLabel);
                return null;
            }
            case TOP -> {
                // Get the stack reference.
                var stackVarAddress = generator.generateVariable(builtinExpression.argumentList.get(0));
                var stackReference = generator.emitLoad(stackVarAddress);

                // And let the runtime generate the top operation.
                return generator.stackImplementation.generateTop(generator, stackReference);
            }
            default -> throw Builtins.builtinNotImplemented(builtinExpression.expression);
        }
    }

    @Override
    public Operand visit(IntLiteral intLiteral) {
        // TODO: This would fail if a constant expression like "4 + 2" would be translated to ECO32 code.
        // Since there is no support for a "constant + constant" addressing mode.
        return new ConstantOperand(intLiteral.value);
    }

    @Override
    public Operand visit(StackNilLiteral stackNilLiteral) {
        return generator.stackNil();
    }

    @Override
    public Operand visit(VariableExpression variableExpression) {
        var variable = generator.generateVariable(variableExpression.variable);
        return generator.emitLoad(variable);
    }
}
