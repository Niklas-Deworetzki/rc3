package rc3.stackmachine;

import rc3.januscompiler.Direction;
import rc3.januscompiler.syntax.AssignStatement;
import rc3.lib.TieredReversibleSink;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.ParamView;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.VoidRSSAVisitor;
import rc3.stackmachine.syntax.Instruction;
import rc3.stackmachine.syntax.Line;
import rc3.stackmachine.syntax.Operand;

import java.util.*;
import java.util.function.Consumer;

import static rc3.lib.utils.CollectionUtils.reverse;
import static rc3.lib.utils.SetOperations.concat;
import static rc3.rssa.instances.BinaryOperand.BinaryOperator;

public final class Translation extends VoidRSSAVisitor {
    private final TieredReversibleSink<Line> output;
    private final ControlGraph currentProcedure;
    private final Map<String, ConditionEvaluation.TargetValue> conditionTargets;
    private final Map<String, int[]> movableParameters;

    private Translation(ControlGraph currentProcedure, Consumer<Line> lineConsumer) {
        this.output = new TieredReversibleSink<>(lineConsumer, Line::reverse);
        this.currentProcedure = currentProcedure;

        this.conditionTargets = ConditionEvaluation.findConsentForConditions(currentProcedure);
        this.movableParameters = ConnectedConditions.parametersRequiringMove(currentProcedure);
    }

    public static void ofProgram(Program program, Consumer<Line> lineConsumer) {
        for (Line line : new AnnotationTranslator(program)) {
            lineConsumer.accept(line);
        }

        translateEntryPoint(program, lineConsumer);

        final String entryPoint = program.getEntryPoint();
        for (var procedure : program.procedures().entrySet()) {
            if (entryPoint.equals(procedure.getKey()))
                continue; // Skip entry, as it was already translated.

            Translation.translateProcedure(procedure.getValue(), lineConsumer);
        }
    }

    private static void translateEntryPoint(Program program, Consumer<Line> lineConsumer) {
        ControlGraph entryProcedure = program.getProcedure(program.getEntryPoint());
        Translation translation = new Translation(entryProcedure, lineConsumer);

        translation.generateEntryCode();
        entryProcedure.toCode().forEach(translation);
    }

    private static void translateProcedure(ControlGraph procedure, Consumer<Line> lineConsumer) {
        Translation translation = new Translation(procedure, lineConsumer);
        procedure.toCode().forEach(translation);
    }


    private void generateReverse(Runnable action) {
        output.generateReverse(action);
    }

    private void doEmitInstruction(Instruction instruction) {
        output.accept(instruction);
    }


    private void emitFunctionEntry(String functionLabel) {
        final Instruction instruction = new Instruction("call");
        instruction.addLabel(new Operand.Symbol(functionLabel));
        doEmitInstruction(instruction);
    }

    private void emitInstructionWithLabel(Operand.Symbol label, String mnemonic, Operand operand) {
        final Instruction instruction = new Instruction(mnemonic, Optional.of(operand));
        instruction.addLabel(label);
        doEmitInstruction(instruction);
    }

    private void emitInstruction(String mnemonic) {
        doEmitInstruction(new Instruction(mnemonic));
    }

    private void emitInstruction(String mnemonic, Operand operand) {
        doEmitInstruction(new Instruction(mnemonic, Optional.of(operand)));
    }

    private void emitInstruction(String mnemonic, int constant) {
        doEmitInstruction(new Instruction(mnemonic, Optional.of(new Operand.Constant(constant))));
    }

    /**
     * The minimum value representable as the absolute operand of an instruction.
     */
    private static final int MIN_VALUE = Short.MIN_VALUE;
    /**
     * The maximum value representable as the absolute operand of an instruction.
     */
    private static final int MAX_VALUE = Short.MAX_VALUE;

    private boolean isConstantValidOperand(int constant) {
        return constant >= MIN_VALUE && constant <= MAX_VALUE;
    }

    /**
     * Determines whether a move is required when transferring one {@link Atom} to another.
     * <p>
     * Moving an {@link Atom} usually requires evaluation of the source, which places the
     * corresponding value onto the stack and consequent inverse evaluation of the target,
     * which consumes the value from the stack, assigning it to an {@link Atom}.
     * <p>
     * These operations can be omitted, if:
     * <ul>
     *  <li>Both operands are a {@link Variable} inhabiting the same stack slot within an
     *  stack frame. In this case, the move would just push from the stack frame onto the
     *  stack and then pop from the stack into the same stack frame, which is redundant.</li>
     *  <li>Both operands represent the same {@link Constant}. In this case, the constant
     *  would be pushed onto the stack only to be removed afterwards, which is redundant.</li>
     * </ul>
     * Every other combination of {@link Variable} and {@link Constant} operands must
     * be moved, since a move can either detect runtime errors, assign a value to a
     * variable or lets values change the stack slots it's inhabiting.
     *
     * <p>
     * The relation implemented by this method has the following properties:
     * <ul>
     * <li>It is <i>antireflexive</i>: for any atom <code>a</code>, <code>requiresMove(a, a)</code>
     * returns <code>false</code>.</li>
     * <li>It is <i>symmetric</i>: for any atoms <code>a</code> and <code>b</code>,
     * <code>requiresMove(a, b)</code> should return <code>true</code> if and only if
     * <code>requiresMove(b, a)</code> returns <code>true</code>.</li>
     * <li>It is <i>transitive</i>: for any atoms <code>a</code>, <code>b</code> and <code>c</code>,
     * if <code>requiresMove(a, b)</code> returns <code>true</code> and <code>requiresMove(b, c)</code>
     * returns <code>true</code>, then <code>requiresMove(a, c)</code> should also return <code>true</code>.
     * <li>It is <i>consistent</i>: for any atoms <code>a</code> and <code>b</code>, multiple invocations
     * <code>requiresMove(a, b)</code> consistently return <code>true</code> or consistently return
     * <code>false</code>, provided non of the atoms are a {@link Variable} and their {@link Variable#offset}
     * is modified in between invocations.
     * </ul>
     */
    static boolean requiresMove(Atom source, Atom target) {
        if (source instanceof Variable sourceVar &&
                target instanceof Variable targetVar) {
            return sourceVar.offset != targetVar.offset;
        }
        // They might both be constants with equal value.
        return !source.equals(target);
    }


    @Override
    protected void visitVoid(Constant constant) {
        final int value = constant.getValue();

        emitInstruction("pushc", value);
        if (!isConstantValidOperand(value)) {
            // Constant is too large to fit in a single instruction.
            emitInstruction("xorhc", value);
        }
    }

    @Override
    protected void visitVoid(Variable variable) {
        emitInstruction("pushl", variable.offset + 1);
    }

    @Override
    protected void visitVoid(MemoryAccess memoryAccess) {
        if (memoryAccess.index instanceof Constant constant &&
                isConstantValidOperand(constant.getValue())) { // Load is a constant address that fits in an operand.
            emitInstruction("pushm", constant.getValue());
        } else {
            memoryAccess.index.accept(this);
            emitInstruction("load");
            emitInstruction("swap");
            generateReverse(() -> memoryAccess.index.accept(this));
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // Procedures & Calls
    //  We use the proposed scheme for procedure calls that is adapted from
    //  PISA. Every procedure has a single entry point, that is also the
    //  exit point. First and last instruction of a procedure are branches,
    //  referencing each other. The prologue or epilogue has to negate the
    //  call offset, so a returning call is possible. We do it in prologue.
    //
    //  Stack frames are allocated as part of the prologue and destroyed in
    //  the epilogue. Returning values is the same code as loading params,
    //  just executed backwards.
    //
    //  Parameters are passed with the begin of the parameter list on top
    //  of the stack. Space reserved for a longer parameter list is placed
    //  below. This way, parameters are always addressable by their index
    //  in any parameter list.
    //
    //  Layout:
    //  ----x------x------x------x------x------x------x------x--
    //      | var3 | var2 | var1 |  fp  | ret  | par1 | par2 | ...
    //  ----x------x------x------x------x------x------x------x--
    //   ^   fp + 3 fp + 2 fp + 1        fp - 1 fp - 2 fp - 3
    //   sp
    //
    ////////////////////////////////////////////////////////////////////////

    private static Operand.Symbol topLabel(String label) {
        return new Operand.Symbol(label + "_top");
    }

    private static Operand.Symbol botLabel(String label) {
        return new Operand.Symbol(label + "_bot");
    }

    /**
     * Push values onto the stack and place them in return area.
     */
    private void storeReturnValues(List<Atom> parameterList) {
        for (int offset = 0; offset < parameterList.size(); offset++) {
            parameterList.get(offset).accept(this);
            emitInstruction("popl", -(2 + offset));
        }
    }

    @Override
    protected void visitVoid(BeginInstruction begin) {
        emitInstructionWithLabel(topLabel(begin.label), "branch", botLabel(begin.label));
        emitInstruction("neg"); // Calculate return offset.
        emitFunctionEntry(begin.label);

        emitInstruction("asf", currentProcedure.frameSize);
        generateReverse(() -> storeReturnValues(begin.getParameters()));
    }

    @Override
    protected void visitVoid(EndInstruction end) {
        storeReturnValues(end.getParameters());
        emitInstruction("rsf", currentProcedure.frameSize);

        emitInstructionWithLabel(botLabel(end.label), "branch", topLabel(end.label));
    }

    @Override
    protected void visitVoid(CallInstruction instruction) {
        final int sizeDifference = instruction.inputList.size() - instruction.outputList.size();

        if (sizeDifference < 0) {
            emitInstruction("allocpar", -sizeDifference);
        }
        for (Atom parameter : reverse(instruction.inputList)) {
            parameter.accept(this);
        }

        emitInstruction("pushc", new Operand.Sub(Operand.of(instruction.procedure), Operand.relative(+1)));
        emitInstruction(instruction.direction.choose("call", "uncall"));
        emitInstruction("popc", new Operand.Sub(Operand.relative(-1), Operand.of(instruction.procedure)));


        for (Atom parameter : instruction.outputList) {
            generateReverse(() -> parameter.accept(this));
        }
        if (sizeDifference > 0) {
            emitInstruction("releasepar", sizeDifference);
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // Swap Instruction
    //  The swap instruction is optimized to check, if two swapped atoms are
    //  variables in the same slot. If they are, they do not have to be
    //  loaded and stored.
    //
    //  We always have to push ALL swapped variables onto the stack.
    //  Otherwise we would run into errors, if two variables swap their
    //  stack slots.
    ////////////////////////////////////////////////////////////////////////

    @Override
    protected void visitVoid(SwapInstruction instruction) {
        final boolean lSwap = requiresMove(instruction.sourceLeft, instruction.targetLeft);
        final boolean rSwap = requiresMove(instruction.sourceRight, instruction.targetRight);

        if (lSwap && rSwap) {
            instruction.sourceLeft.accept(this);
            instruction.sourceRight.accept(this);
            generateReverse(() -> instruction.targetRight.accept(this));
            generateReverse(() -> instruction.targetLeft.accept(this));
        } else if (lSwap) {
            instruction.sourceLeft.accept(this);
            generateReverse(() -> instruction.targetLeft.accept(this));
        } else if (rSwap) {
            instruction.sourceRight.accept(this);
            generateReverse(() -> instruction.targetRight.accept(this));
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // Assignments
    //  Computationally powerful RSSA instructions require many generated
    //  stack instructions. Therefore we optimize assignments as hard as
    //  possible to generate minimal stack instructions.
    //
    //  RSSA operators are mapped to stack instructions. Some comparisons
    //  have to be inverted.
    //
    // The implementation abstracts over memory assignments and variable
    // assignments. M[x] += a * b are written as M[x] := M[x] + (a * b).
    // This allows garbage of the operand to remain on the stack during
    // the assignment. The general translation for x := y + (a * b) is:
    //   gen(a * b)
    //   gen(y)
    //   +           ; In-Place modification of y or x.
    //   fin(x)
    //   fin(a * b)
    // where gen creates a value and fin finalizes it (inverse of gen).
    //
    // There are however a few optimizations, in which we can generate fewer
    // instructions:
    //  1. x, y, a, b are constant.
    //    If the left and right-hand side are equivalent, no code has to be
    //    generated.
    //  2. x, a, b are constant.
    //  3. y, a, b are constant.
    //    Assign the constant part to the variable.
    //  4. a, b are constant with c = a * b
    //    Assign x := y + c
    //  5. x := 0 + a * b or x := 0 ^ a * b
    //  6. 0 := y - a * b or 0 := y ^ a * b
    //    Assignment duplicates constant or finalizes with a constant.
    //  7. x := y + 1 * b
    //  8. x := y + a * 1
    //    Assign x := y + e where e is the other element. Since it is not
    //    changed by operation with neutral element.
    //  9. x := y + a - c, where c is a constant.
    //    Use dec instruction to compute (a - c)
    //  10. x := y + (a + c)
    //  11. x := y + (c + b)
    //    Use inc instruction to compute + c
    ////////////////////////////////////////////////////////////////////////

    private record BinaryOperatorData(BinaryOperator operator, String instruction, boolean isInverted) {
        private BinaryOperatorData(BinaryOperator operator, String instruction) {
            this(operator, instruction, false);
        }
    }

    /**
     * Map assignment operators to instructions. These are always in-place instructions.
     */
    private static final Map<AssignStatement.ModificationOperator, String> ASSIGNMENT_INSTRUCTIONS = Map.of(
            AssignStatement.ModificationOperator.ADD, "add",
            AssignStatement.ModificationOperator.SUB, "sub",
            AssignStatement.ModificationOperator.XOR, "xor"
    );

    /**
     * Map binary operators to instructions.
     */
    private static final Map<BinaryOperator, BinaryOperatorData> ARITHMETIC_INSTRUCTIONS = new EnumMap<>(BinaryOperator.class);
    private static final List<BinaryOperatorData> OPERATOR_DATA = List.of(
            new BinaryOperatorData(BinaryOperator.ADD, "add"),
            new BinaryOperatorData(BinaryOperator.SUB, "sub"),
            new BinaryOperatorData(BinaryOperator.XOR, "xor"),
            new BinaryOperatorData(BinaryOperator.SAL, "shl"),
            new BinaryOperatorData(BinaryOperator.SAR, "shr"),

            new BinaryOperatorData(BinaryOperator.MUL, "arpushmul"),
            new BinaryOperatorData(BinaryOperator.DIV, "arpushdiv"),
            new BinaryOperatorData(BinaryOperator.MOD, "arpushmod"),
            new BinaryOperatorData(BinaryOperator.OR, "arpushor"),
            new BinaryOperatorData(BinaryOperator.AND, "arpushand"),

            new BinaryOperatorData(BinaryOperator.EQU, "cmpusheq"),
            new BinaryOperatorData(BinaryOperator.LST, "cmpushlt"),
            new BinaryOperatorData(BinaryOperator.GRT, "cmpushlt", true),
            new BinaryOperatorData(BinaryOperator.NEQ, "cmpushne"),
            new BinaryOperatorData(BinaryOperator.LSE, "cmpushle"),
            new BinaryOperatorData(BinaryOperator.GRE, "cmpushle", true)
    );

    static {
        for (BinaryOperatorData operatorDatum : OPERATOR_DATA) {
            ARITHMETIC_INSTRUCTIONS.put(operatorDatum.operator, operatorDatum);
        }
    }


    @Override
    protected void visitVoid(BinaryOperand binaryOperand) {
        // Implemented via pushBinary or pushCondition with many optimizations.
        throw new UnsupportedOperationException();
    }

    private void pushBinary(RValue lhs, BinaryOperator operator, RValue rhs) {
        if (lhs instanceof Constant lConst && operator.isIdentity(lConst)) {
            // lhs is neutral element. Only rhs is relevant.
            rhs.accept(this);

        } else if (rhs instanceof Constant rConst && operator.isIdentity(rConst)) {
            // rhs is neutral element. Only lhs is relevant.
            lhs.accept(this);

        } else if (lhs instanceof Constant lConst && isConstantValidOperand(lConst.getValue()) &&
                operator == BinaryOperator.ADD) {
            // Use increment instruction instead of pushc and add.
            rhs.accept(this);
            emitInstruction("inc", lConst.getValue());

        } else if (rhs instanceof Constant rConst && isConstantValidOperand(rConst.getValue()) &&
                operator == BinaryOperator.ADD) {
            // Use increment instruction instead of pushc and add.
            lhs.accept(this);
            emitInstruction("inc", rConst.getValue());

        } else if (rhs instanceof Constant rConst && isConstantValidOperand(rConst.getValue()) &&
                operator == BinaryOperator.SUB) {
            // Use decrement instruction instead of pushc and sub.
            lhs.accept(this);
            emitInstruction("dec", rConst.getValue());

        } else if (lhs.equals(rhs)) {
            // Duplicate equal operands to prevent work and problems.
            rhs.accept(this);
            emitInstruction("dup");
            emitInstruction(ARITHMETIC_INSTRUCTIONS.get(operator).instruction);

        } else if (lhs instanceof MemoryAccess && rhs instanceof MemoryAccess) {
            // If both indices are equal *at runtime*, pushing a memory cell two times fails.
            // This case if after lhs.equals(rhs) so two statically known indices can be optimized using dup.
            rhs.accept(this);
            emitInstruction("dup");
            generateReverse(() -> rhs.accept(this));

            lhs.accept(this);
            emitInstruction(ARITHMETIC_INSTRUCTIONS.get(operator).instruction);
        } else {
            // Default case, two different operands.
            rhs.accept(this);
            lhs.accept(this);
            emitInstruction(ARITHMETIC_INSTRUCTIONS.get(operator).instruction);
        }
    }

    private void assign(RValue target, RValue source,
                        AssignStatement.ModificationOperator assignment,
                        RValue lhs, BinaryOperator operator, RValue rhs) {
        final OptionalInt constOperand = BinaryOperand.evaluateConstant(lhs, operator, rhs);
        if (constOperand.isPresent()) {
            if (target instanceof Constant tConst && source instanceof Constant sConst) {
                // Assignment is completely constant.
                final int rhsValue = assignment.evaluateConstant(sConst.getValue(), constOperand.getAsInt());
                if (tConst.getValue() != rhsValue) {
                    // Force runtime error if assignment sides are not equal
                    new Constant(rhsValue).accept(this);
                    generateReverse(() -> tConst.accept(this));
                }

            } else if (source instanceof Constant sConst) { // Constant assigned to variable. (Evaluate rhs statically)
                final int rhsVal = assignment.evaluateConstant(sConst.getValue(), constOperand.getAsInt());
                new Constant(rhsVal).accept(this);
                generateReverse(() -> target.accept(this));

            } else if (target instanceof Constant tConst) { // Variable assigned to constant. (Evaluate lhs statically)
                final int lhsVal = assignment.invert().evaluateConstant(tConst.getValue(), constOperand.getAsInt());
                source.accept(this);
                generateReverse(() -> new Constant(lhsVal).accept(this));

            } else if (constOperand.getAsInt() == 0) { // Operand is ignored by assignment.
                if (!(source instanceof Atom sourceAtom) || !(target instanceof Atom targetAtom) ||
                        requiresMove(sourceAtom, targetAtom)) { // If both operands are Atoms, only move if move is required.
                    source.accept(this);
                    generateReverse(() -> target.accept(this));
                }
            } else if (assignment != AssignStatement.ModificationOperator.XOR) {
                source.accept(this);
                switch (assignment) {
                    case ADD -> emitInstruction("inc", constOperand.getAsInt());
                    case SUB -> emitInstruction("sub", constOperand.getAsInt());
                }
                generateReverse(() -> target.accept(this));

            } else { // Operand is constant, evaluate it statically.
                final Constant operand = new Constant(constOperand.getAsInt());
                operand.accept(this);
                source.accept(this);

                emitInstruction(ASSIGNMENT_INSTRUCTIONS.get(assignment));

                generateReverse(() -> {
                    operand.accept(this);
                    target.accept(this);
                });
            }
        } else if (source instanceof Constant sConst && sConst.getValue() == 0 &&
                assignment != AssignStatement.ModificationOperator.SUB) {
            // Source does not change operand value.
            pushBinary(lhs, operator, rhs);
            emitInstruction("dup"); // Target is duplication of source.

            generateReverse(() -> target.accept(this));
            generateReverse(() -> pushBinary(lhs, operator, rhs));

        } else if (target instanceof Constant tConst && tConst.getValue() == 0 &&
                assignment != AssignStatement.ModificationOperator.ADD) {
            // Target does not change operand value.
            pushBinary(lhs, operator, rhs);
            source.accept(this);

            emitInstruction("undup"); // Source is destroyed using undup.
            generateReverse(() -> pushBinary(lhs, operator, rhs));

        } else { // Default case. Generate source, clean up target.
            pushBinary(lhs, operator, rhs);
            source.accept(this);

            emitInstruction(ASSIGNMENT_INSTRUCTIONS.get(assignment));

            generateReverse(() -> {
                pushBinary(lhs, operator, rhs);
                target.accept(this);
            });
        }
    }

    @Override
    protected void visitVoid(ArithmeticAssignment instruction) {
        assign(instruction.target, instruction.source, instruction.operator,
                instruction.left, instruction.arithmeticOperator, instruction.right);
    }

    @Override
    protected void visitVoid(MemoryAssignment instruction) {
        assign(instruction.target, instruction.target, instruction.operator,
                instruction.left, instruction.arithmeticOperator, instruction.right);
    }


    @Override
    protected void visitVoid(MemoryInterchangeInstruction instruction) {
        // Assign memory to lhs.
        instruction.memoryAccess.accept(this);
        generateReverse(() -> instruction.left.accept(this));

        // Assign rhs to memory.
        instruction.right.accept(this);
        generateReverse(() -> instruction.memoryAccess.accept(this));
    }

    @Override
    protected void visitVoid(MemorySwapInstruction instruction) {
        if (!instruction.left.index.equals(instruction.right.index)) {
            instruction.left.index.accept(this);
            instruction.right.index.accept(this);

            emitInstruction("memswap");

            generateReverse(() -> {
                instruction.left.index.accept(this);
                instruction.right.index.accept(this);
            });
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // Entry & Exit Points
    //  Entry and exit points have two tasks:
    //   1. Moving data similar to phi functions in regular SSA
    //   2. Jumping to the correct target
    //
    //  Selecting the correct target is rather simple, since it is encoded
    //  by a static label. However, since there are brt and brf instructions
    //  used to perform conditionals jumps, a label can only be used with a
    //  true or with a false value on the stack. Not both at the same time.
    //  This requires consent on how labels occur within a ControlGraph.
    //  If a jump is only performed between unconditional points, no
    //  condition is needed. Otherwise, unconditional points can push or pop
    //  a constant true or false value.
    //
    //  Evaluation of a condition must occur before data is moved, making
    //  the move more complicated. The condition is on top of the stack,
    //  before the move starts, so the condition must "bubble" up, when
    //  different atoms are passed from the points parameter list. This is
    //  optimized to only use a dig instruction every two parameters instead
    //  of a swap after every parameter, but is still considerable overhead.
    //  Unused moves can be eliminated to further reduce the cost.
    ////////////////////////////////////////////////////////////////////////

    private void passPhiParameters(List<Atom> origin, String label) {
        passPhiParameters(origin, label, Collections.emptySet(), null);
    }

    private void passPhiParameters(List<Atom> origin, String label,
                                   Set<? extends Atom> variablesInCondition,
                                   Runnable conditionGenerator) {
        final int[] parameterIndices = movableParameters.get(label);
        int movedBeforeCondition = 0, movedAfterCondition = 0;

        // Move all parameters that can be placed before the condition.
        while (movedBeforeCondition < parameterIndices.length) {
            Atom parameter = origin.get(parameterIndices[movedBeforeCondition]);

            if (variablesInCondition.contains(parameter)) {
                // We now must place the condition, before any more parameters can be placed.
                break;
            } else {
                parameter.accept(this);
                movedBeforeCondition++;
            }
        }

        if (conditionGenerator != null) {
            conditionGenerator.run();
        }

        // Move all parameters that are placed after the condition.
        while (movedBeforeCondition + movedAfterCondition < parameterIndices.length) {
            Atom parameter = origin.get(parameterIndices[movedBeforeCondition + movedAfterCondition]);
            parameter.accept(this);
            movedAfterCondition++;

            if (movedAfterCondition % 2 == 0) { // Pull the condition back to top.
                emitInstruction("dig");
            }
        }

        if (movedAfterCondition % 2 == 1) { // Condition is buried under a single parameter.
            emitInstruction("swap");
        }
    }

    /**
     * Pairs of comparison operators, that express the inverse comparison.
     */
    private static final Map<BinaryOperator, BinaryOperator> INVERSE_COMPARATORS = new EnumMap<>(BinaryOperator.class);
    private static final List<Map.Entry<BinaryOperator, BinaryOperator>> INVERSE_COMPARATOR_PAIRS = List.of(
            Map.entry(BinaryOperator.EQU, BinaryOperator.NEQ),
            Map.entry(BinaryOperator.LST, BinaryOperator.GRE),
            Map.entry(BinaryOperator.LSE, BinaryOperator.GRT)
    );

    static {
        for (var operatorPair : INVERSE_COMPARATOR_PAIRS) {
            INVERSE_COMPARATORS.put(operatorPair.getKey(), operatorPair.getValue());
            INVERSE_COMPARATORS.put(operatorPair.getValue(), operatorPair.getKey());
        }
    }

    private void pushCondition(RValue lhs, BinaryOperator comparator, RValue rhs) {
        if (comparator.isComparison() && lhs instanceof Constant lConst && rhs instanceof Constant rConst) {
            // Perform a constant comparison, as both operands are constant.
            if (comparator.compareConstant(lConst.getValue(), rConst.getValue())) {
                emitInstruction("pushtrue");
            } else {
                emitInstruction("pushfalse");
            }
        } else if (lhs.equals(rhs)) {
            // Try to perform a constant comparison, as operands are identical.
            switch (comparator) {
                case LSE, GRE, EQU -> emitInstruction("pushtrue");
                case LST, GRT, NEQ -> emitInstruction("pushfalse");
                default -> {
                    // Special case, we cannot evaluate comparison, but both operands could be variables.
                    rhs.accept(this);
                    emitInstruction("dup");

                    // No need to check isInverted, since those cases are earlier caught by switch.
                    emitInstruction(ARITHMETIC_INSTRUCTIONS.get(comparator).instruction);
                    emitInstruction("bury");
                    emitInstruction("undup");
                    generateReverse(() -> rhs.accept(this));
                }
            }
        } else {
            final var operator = ARITHMETIC_INSTRUCTIONS.get(comparator);
            List<Line> operands = output.generatedBy(() -> {
                if (operator.isInverted) {
                    lhs.accept(this);
                    rhs.accept(this);
                } else {
                    rhs.accept(this);
                    lhs.accept(this);
                }
            });

            output.acceptAll(operands);
            emitInstruction(operator.instruction);
            emitInstruction("bury");
            output.generateReverse(() -> output.acceptAll(operands));
        }
    }


    private void exitUnconditional(String label, Direction origin, List<Atom> parameters) {
        Operand.Symbol declared, targeted;
        if (origin.isForward()) {
            declared = botLabel(label);
            targeted = topLabel(label);
        } else {
            declared = topLabel(label);
            targeted = botLabel(label);
        }

        passPhiParameters(parameters, label);

        switch (conditionTargets.get(label)) {
            case TRUE -> emitInstruction("pushtrue");
            case FALSE -> emitInstruction("pushfalse");
        }

        emitInstructionWithLabel(declared, "branch", targeted);
    }

    private void exitConditional(Direction origin, List<Atom> parameters,
                                 String lhsLabel, String rhsLabel,
                                 RValue lhs, BinaryOperator comparator, RValue rhs) {
        Operand.Symbol declaredLhs, targetedLhs, declaredRhs, targetedRhs;
        if (origin.isForward()) {
            declaredLhs = botLabel(lhsLabel);
            declaredRhs = botLabel(rhsLabel);
            targetedLhs = topLabel(lhsLabel);
            targetedRhs = topLabel(rhsLabel);
        } else {
            declaredLhs = topLabel(lhsLabel);
            declaredRhs = topLabel(rhsLabel);
            targetedLhs = botLabel(lhsLabel);
            targetedRhs = botLabel(rhsLabel);
        }

        final boolean reversedCondition = conditionTargets.get(lhsLabel) == ConditionEvaluation.TargetValue.FALSE;

        final Set<Variable> variablesUsedInCondition = new HashSet<>(concat(lhs.variablesUsed(), rhs.variablesUsed()));
        passPhiParameters(parameters, lhsLabel, variablesUsedInCondition, () -> {
            // Consent for condition is inverse to declared condition.
            if (!reversedCondition) {
                pushCondition(lhs, comparator, rhs);
            } else {
                pushCondition(lhs, INVERSE_COMPARATORS.get(comparator), rhs);
            }
        });

        if (!reversedCondition) {
            emitInstructionWithLabel(declaredLhs, "brt", targetedLhs);
            emitInstructionWithLabel(declaredRhs, "brf", targetedRhs);
        } else {
            emitInstructionWithLabel(declaredLhs, "brf", targetedLhs);
            emitInstructionWithLabel(declaredRhs, "brt", targetedRhs);
        }
    }

    @Override
    protected void visitVoid(UnconditionalEntry unconditional) {
        generateReverse(() ->
                exitUnconditional(unconditional.label, Direction.BACKWARD, unconditional.getParameters()));
    }

    @Override
    protected void visitVoid(UnconditionalExit unconditional) {
        exitUnconditional(unconditional.label, Direction.FORWARD, unconditional.getParameters());
    }


    @Override
    protected void visitVoid(ConditionalEntry conditional) {
        generateReverse(() ->
                exitConditional(Direction.BACKWARD, conditional.getParameters(),
                        conditional.trueLabel, conditional.falseLabel,
                        conditional.left, conditional.operator, conditional.right));
    }

    @Override
    protected void visitVoid(ConditionalExit conditional) {
        exitConditional(Direction.FORWARD, conditional.getParameters(),
                conditional.trueLabel, conditional.falseLabel,
                conditional.left, conditional.operator, conditional.right);
    }


    ////////////////////////////////////////////////////////////////////////
    // Startup Code
    //  Generate a procedure call to the current procedure, that will push
    //  constants according to @Param annotations (if present).
    //  Return values are not eliminated, but stay on stack and a halt
    //  instruction is executed.
    ////////////////////////////////////////////////////////////////////////

    private void generateEntryCode() {
        emitInstruction( "start");

        BeginInstruction entryPoint = currentProcedure.getBeginInstruction();
        int returnedValues = currentProcedure.getEndInstruction().getParameters().size();

        if (returnedValues > entryPoint.getParameters().size()) {
            emitInstruction("allocpar", returnedValues - entryPoint.getParameters().size());
        }

        for (Atom parameter : reverse(entryPoint.getParameters())) {
            Constant valueForParameter = parameter.findAnnotation("Param")
                    .<ParamView>map(Annotation::getView)
                    .map(ParamView::getValue)
                    .orElse(new Constant(0));
            valueForParameter.accept(this);
        }

        final Operand.Symbol targetProcedure = Operand.of(entryPoint.label);
        emitInstruction("pushc", new Operand.Sub(targetProcedure, Operand.relative(+1)));
        emitInstruction("call");
        emitInstruction("popc", new Operand.Sub(Operand.relative(-1), targetProcedure));

        emitInstruction("stop");
    }
}
