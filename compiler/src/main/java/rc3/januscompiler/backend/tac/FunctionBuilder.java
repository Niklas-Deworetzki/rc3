package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.tac.types.CArrayType;
import rc3.januscompiler.backend.tac.types.CPointerType;
import rc3.januscompiler.backend.tac.types.CPrimitiveType;
import rc3.januscompiler.backend.tac.types.CType;
import rc3.januscompiler.tac.instances.*;
import rc3.januscompiler.tac.visitor.DoNothingVoidTacVisitor;
import rc3.lib.messages.ErrorMessage;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;

/**
 * This class is used to construct a collection of {@link CFunction CFunctions} from a {@link List} of
 * {@link Instruction Instructions}.
 * <p>
 * The static {@link FunctionBuilder#buildFunctionsFrom(List)} method is used to construct a list of
 * {@link CFunction CFunctions}.
 * </p>
 * <p>
 * The analysis of {@link Instruction Instructions} also infers a type for all temporary variables and
 * adds them to {@link CFunction#temporaryVariables}.
 * </p>
 */
public class FunctionBuilder {
    private List<CFunction> detectedFunctions;
    private CFunction activeFunction;

    /**
     * This map contains types of available members. A {@link CType} is bound to a map
     * from member name to the {@link CType} of the member.
     */
    private static final Map<CType, Map<String, CType>> builtinMembers = Map.of(
            CPrimitiveType.StackType, Map.of(
                    "predecessor", CPrimitiveType.StackType,
                    "value", CPrimitiveType.IntType,
                    "refcount", CPrimitiveType.IntType)
    );

    /**
     * Scans the given {@link List} of {@link Instruction Instructions} for {@link CFunction CFunctions} and
     * collects them into the list returned.
     *
     * @param instructionList The {@link List} of {@link Instruction Instructions}
     *                        to scan for {@link CFunction CFunctions}.
     * @return A {@link List} of {@link CFunction CFunctions} found.
     */
    public List<CFunction> buildFunctionsFrom(List<Instruction> instructionList) {
        detectedFunctions = new ArrayList<>();
        var builder = new BuilderVisitor();

        for (Instruction instruction : instructionList) {
            instruction.accept(builder);
            if (activeFunction != null) {
                activeFunction.body.add(instruction);
            }
        }

        return detectedFunctions;
    }

    /**
     * Creates a {@link Predicate} to check whether an {@link Instruction} is a {@link Label} with given name.
     *
     * @param procedureName The name a label must have to match the {@link Predicate}.
     * @return A {@link Predicate} to check for {@link Label Labels} with a given name.
     */
    protected Predicate<Instruction> isProcedureLabel(String procedureName) {
        return instruction -> instruction instanceof Label &&
                Objects.equals(procedureName, ((Label) instruction).name);
    }

    /**
     * Marks the beginning of a new {@link CFunction}.
     *
     * @param beginInstruction The {@link BeginInstruction} marking the start of a new {@link CFunction}.
     */
    protected void startNewFunction(BeginInstruction beginInstruction) {
        if (activeFunction != null) {
            // Cleanup instructions collected for function. The label of the next procedure may be still in there.
            String name = beginInstruction.procedureLabel;
            activeFunction.body.removeIf(isProcedureLabel(name));
        }

        // Register a new function.
        activeFunction = new CFunction(beginInstruction.procedureLabel, beginInstruction.procedure);
        detectedFunctions.add(activeFunction);

        // Add all known variables to the function.
        for (var parameter : beginInstruction.procedure.parameters) {
            activeFunction.parameters.add(CVariable.fromVariable(parameter));
        }
        for (var localVariable : beginInstruction.procedure.localDeclarations) {
            activeFunction.localVariables.add(CVariable.fromVariable(localVariable));
        }
        for (var scope : beginInstruction.procedure.body) {
            for (var variable : scope.allVariables()) {
                activeFunction.localVariables.add(CVariable.fromVariable(variable));
            }
        }
    }

    /**
     * Checks whether the given {@link ConstantOperand} represents the nil reference in C.
     * <p>
     * An {@link ConstantOperand} represents <code>nil</code>, if its representation
     * obtained via {@link Constant#getRepresentation()} equals the {@link String}
     * <code>"nil"</code>.
     * </p>
     *
     * @param operand The {@link ConstantOperand} to check.
     * @return <code>true</code> if the {@link ConstantOperand} represents <code>nil</code>.
     */
    protected boolean isNil(ConstantOperand operand) {
        return operand.value.getRepresentation().equals("nil");
    }

    /**
     * Returns the {@link CType} of an {@link Operand} if known. If the {@link Operand} is not a
     * {@link VariableOperand} or the variable is not known, no {@link CType} can be returned.
     *
     * @param operand The {@link Operand} to find the type of.
     * @return The {@link CType} of the {@link Operand} or empty.
     */
    protected Optional<CType> typeOf(Operand operand) {
        if (activeFunction != null && operand instanceof VariableOperand) {
            String name = ((VariableOperand) operand).name;
            return activeFunction.findVariable(name)
                    .map(CVariable::type);

        } else if (operand instanceof ConstantOperand && isNil((ConstantOperand) operand)) {
            return Optional.of(CPrimitiveType.StackType);
        }

        return Optional.empty();
    }

    /**
     * Annotates a {@link TemporaryOperand} with an inferred type and adds it to the set of known temporary
     * operands for the active function.
     *
     * @param operand The {@link TemporaryOperand} to add.
     * @param type    The {@link CType} inferred for the operand.
     */
    protected void enterTemporaryName(TemporaryOperand operand, CType type) {
        if (activeFunction == null) {
            return; // No active function. Loose instructions outside of a function?
        }

        CVariable variable = new CVariable(operand.name, type);
        activeFunction.temporaryVariables.add(variable);
    }

    /**
     * This {@link DoNothingVoidTacVisitor Visitor} is used to find the {@link BeginInstruction} at the start
     * of each {@link CFunction}. It also infers a {@link CType} for all encountered
     * {@link TemporaryOperand TemporaryOperands} and adds them to {@link CFunction#temporaryVariables}.
     */
    private class BuilderVisitor extends DoNothingVoidTacVisitor {

        @Override
        public void visitVoid(AddressOfInstruction instruction) {
            // AddressOf requires named operands. Hence there will always be an entry present.
            CType operandType = typeOf(instruction.variable).orElseThrow();

            CType resultType = (operandType instanceof CArrayType)
                    ? new CPointerType(CPrimitiveType.IntType) // Janus Arrays are pointers to the first elements.
                    : new CPointerType(operandType);
            enterTemporaryName(instruction.target, resultType);
        }

        @SuppressWarnings("OptionalIsPresent") // Improves readability.
        @Override
        public void visitVoid(ArithmeticInstruction instruction) {
            Optional<CType> lhsType = typeOf(instruction.lhs);
            Optional<CType> rhsType = typeOf(instruction.rhs);

            CType resultType;
            if (lhsType.isPresent() && rhsType.isPresent()) {
                if (lhsType.get() == CPrimitiveType.IntType && rhsType.get() == CPrimitiveType.IntType) {
                    // Int + Int -> Int
                    resultType = CPrimitiveType.IntType;
                } else if (lhsType.get() == CPrimitiveType.IntType) {
                    // Int + Pointer -> Pointer
                    resultType = rhsType.get();
                } else if (rhsType.get() == CPrimitiveType.IntType) {
                    // Pointer + Int -> Pointer
                    resultType = lhsType.get();
                } else {
                    resultType = CPrimitiveType.IntType;
                }
            } else if (lhsType.isPresent()) {
                resultType = lhsType.get();
            } else if (rhsType.isPresent()) {
                resultType = rhsType.get();
            } else {
                resultType = CPrimitiveType.IntType;
            }

            enterTemporaryName(instruction.target, resultType);
        }

        @Override
        public void visitVoid(BeginInstruction instruction) {
            startNewFunction(instruction);
        }

        @Override
        public void visitVoid(CopyInstruction instruction) {
            CType operandType = typeOf(instruction.source).orElse(CPrimitiveType.IntType);
            enterTemporaryName(instruction.target, operandType);
        }

        /**
         * Checks whether a given {@link Constant} represents an accessed member.
         * <p>
         * A {@link Constant} represents a member if its representation obtained via
         * {@link Constant#getRepresentation()} starts with the <i>points to</i> operator (<code>-></code>)
         * followed by any name.
         * </p>
         * <p>
         * The {@link String} starting after the <i>points to</i> operator is the member name that can
         * be acquired using {@link BuilderVisitor#getMemberName(Constant)}
         * </p>
         *
         * @param constant The {@link Constant} to check.
         * @return <code>true</code> if the {@link Constant} represents an accessed member.
         */
        protected boolean isMember(Constant constant) {
            return constant.getRepresentation().startsWith("->");
        }

        /**
         * Returns the accessed members name from an {@link Constant} representing
         * a member access.
         * <p>
         * The member name is the {@link String} starting after the <i>points to</i> operator
         * in the representation of a {@link Constant}.
         * </p>
         *
         * @param constant The {@link Constant} that represents a member access.
         * @return The {@link Constant Constants} representation without the leading operator.
         */
        protected String getMemberName(Constant constant) {
            return constant.getRepresentation().substring("->".length());
        }

        protected CType getMemberType(CType structType, String memberName) throws ErrorMessage.InternalError {
            var availableMembers = builtinMembers.getOrDefault(structType, emptyMap());
            var memberType = availableMembers.get(memberName);
            if (memberType == null) {
                throw new ErrorMessage.InternalError(String.format(
                        "'%s' is not a known member of type %s.", memberName, structType));
            }
            return memberType;
        }

        @Override
        public void visitVoid(LoadInstruction instruction) {
            Optional<CType> operandType = typeOf(instruction.source);

            // Guess Int if no other information is present.
            CType resultType = CPrimitiveType.IntType;
            if (operandType.isPresent()) {
                if (isMember(instruction.offset)) {
                    String memberName = getMemberName(instruction.offset);
                    resultType = getMemberType(operandType.get(), memberName);
                } else if (operandType.get() instanceof CPointerType pointerType) {
                    resultType = pointerType.elementType;
                }
            }

            enterTemporaryName(instruction.target, resultType);
        }
    }
}
