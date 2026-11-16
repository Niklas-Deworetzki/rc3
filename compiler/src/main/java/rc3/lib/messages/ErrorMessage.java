package rc3.lib.messages;

import org.fusesource.jansi.Ansi;
import rc3.januscompiler.syntax.Declaration;
import rc3.januscompiler.syntax.Expression;
import rc3.januscompiler.syntax.VariableDeclaration;
import rc3.januscompiler.table.Identifier;
import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.parsing.Position;
import rc3.rssa.instances.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A subclass of {@link RuntimeException} that is used to report errors during compilation.
 * <p>
 * Instances of this class can not only be provided with an error message, but also an {@link Position} referencing
 * the part of the source code that caused the error.
 * </p>
 * <p>
 * For additional guidance expected and actual values can be provided.
 * </p>
 */
public abstract class ErrorMessage extends RuntimeException implements Message {
    private final Position position;
    private final Object expected, actual;

    protected ErrorMessage(String message, Position position, Object expected, Object actual) {
        super(message);
        this.position = position;
        this.expected = expected;
        this.actual = actual;
    }

    public ErrorMessage(String message, Position position) {
        this(message, position, NO_VALUE, NO_VALUE);
    }

    @Override
    public String header() {
        return "Error";
    }

    @Override
    public Ansi.Color color() {
        return Ansi.Color.RED;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public List<Map.Entry<String, Object>> additionalInfo() {
        return List.of(
                Map.entry("Expected", expected),
                Map.entry("Actual", actual));
    }

    @Override
    public String toString() {
        return Message.toString(this);
    }

    /**
     * Returns an {@link Supplier} that creates new {@link InternalError} instances with the given
     * message. The message passed to this method is a format string, that is formatted using the
     * additional parameters.
     *
     * @see InternalError
     * @see String#format(String, Object...)
     */
    public static Supplier<InternalError> newInternalError(final String message, Object... args) {
        return () -> new InternalError(String.format(message, args));
    }

    /**
     * An internal error caused by a programming error, misconfiguration or another {@link Exception}.
     */
    public static class InternalError extends ErrorMessage {
        public InternalError(String message) {
            super(message, null);
        }

        public InternalError(String message, Throwable cause) {
            super(message, null);
            initCause(cause);
        }
    }

    public static class UnsupportedOperation extends ErrorMessage {
        public UnsupportedOperation(String message, Position position) {
            super("Unable to perform unsupported operation: " + message, position);
        }
    }

    /**
     * An illegal character was encountered in the input file.
     */
    public static class IllegalCharacter extends ErrorMessage {
        private static String stringRepresentation(char character) {
            // Don't display whitespace and control characters, since they can mess with console output.
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                return String.format("0x%X", (int) character);
            } else {
                return String.format("'%c'", character);
            }
        }

        public IllegalCharacter(char offendingChar, Position pos) {
            super(String.format("Illegal character %s detected in input.", stringRepresentation(offendingChar)), pos);
        }
    }

    /**
     * An invalid literal was encountered in the input file.
     */
    public static class InvalidLiteral extends ErrorMessage {
        public InvalidLiteral(String what, String literal, Position pos) {
            super(String.format("Literal %s does not represent a valid %s.", literal, what), pos);
        }
    }

    /**
     * A syntax error was encountered in the input file.
     */
    public static class SyntaxError extends ErrorMessage {
        private static Object formatExpectedTokens(Iterable<String> expectedTokenNames) {
            String stringRepresentation = String.join(", ", expectedTokenNames);
            if (stringRepresentation.isEmpty()) {
                return NO_VALUE;
            } else {
                return stringRepresentation;
            }
        }

        public SyntaxError(Position pos, Iterable<String> expectedTokenNames) {
            this(pos, "Syntax error detected.", expectedTokenNames);
        }

        public SyntaxError(Position pos, String message, Iterable<String> expectedTokenNames) {
            super(message, pos, formatExpectedTokens(expectedTokenNames), NO_VALUE);
        }
    }

    /**
     * A Janus syntax error. A statement list cannot be empty in Janus. Instead, a skip statement must be used.
     */
    public static class EmptyStatementList extends SyntaxError {
        public EmptyStatementList(Position pos) {
            super(pos, "Empty statement lists are not allowed. A skip statement must be included.", List.of("skip"));
        }
    }

    /**
     * A Janus syntax error. A local statement is not closed with a delocal declaration.
     */
    public static class UnclosedLocal extends SyntaxError {
        public UnclosedLocal(Position pos) {
            super(pos, "Missing 'delocal' declaration. The 'local' declaration is not closed correctly.", List.of("delocal"));
        }
    }

    /**
     * An array of negative size was encountered in the input file.
     */
    public static class NegativeArraySize extends ErrorMessage {
        public NegativeArraySize(Identifier array, int size) {
            super(String.format("Variable '%s' cannot be declared as an array of negative size (%d).", array, size),
                    array.position);
        }
    }

    /**
     * No procedure named 'main' has been declared.
     */
    public static class MissingMainProcedure extends ErrorMessage {
        public MissingMainProcedure() {
            super("Main procedure is missing.",
                    null);
        }
    }

    /**
     * A name has been declared multiple times.
     */
    public static class RedeclarationAs extends ErrorMessage {
        public RedeclarationAs(String what, Declaration<?> secondDeclaration) {
            this(what, secondDeclaration.name.name, secondDeclaration.position);
        }

        public RedeclarationAs(String what, String name, Position position) {
            super(String.format("The %s '%s' cannot be declared a second time.", what, name), position);
        }
    }

    /**
     * A {@link Type} is not allowed in a certain context.
     */
    public static class TypeRestrictions extends ErrorMessage {
        public TypeRestrictions(Type type, String where, Position position) {
            super(String.format("Type '%s' is not allowed in %s.", type, where), position);
        }
    }

    /**
     * Not enough arguments have been provided to a called procedure.
     */
    public static class NotEnoughArguments extends ErrorMessage {
        public NotEnoughArguments(Identifier procName, int expectedArgumentCount, int actualArgumentCount, Position position) {
            super(String.format("Not enough arguments are passed to procedure '%s'.", procName), position,
                    expectedArgumentCount, actualArgumentCount);
        }
    }

    /**
     * To many arguments have been provided to a called procedure.
     */
    public static class TooManyArguments extends ErrorMessage {
        public TooManyArguments(Identifier procName, int expectedArgumentCount, int actualArgumentCount, Position position) {
            super(String.format("Too many arguments are passed to procedure '%s'.", procName), position,
                    expectedArgumentCount, actualArgumentCount);
        }
    }

    /**
     * The type of an argument does not match the defined parameter type.
     */
    public static class ParameterTypeMismatch extends ErrorMessage {
        public ParameterTypeMismatch(Identifier procName, int parameter, Type expectedType, Type actualType, Position position) {
            super(String.format("Parameter %d passed to '%s' has the wrong type.", parameter, procName),
                    position, expectedType, actualType);
        }
    }

    /**
     * A construct combines different types that should be equal.
     */
    public static class CombinesDifferentTypes extends ErrorMessage {
        public CombinesDifferentTypes(String what, Position pos, Type t1, Type t2) {
            super(String.format("%s combines different types: %s and %s are not equal.", what, t1, t2), pos);
        }
    }

    /**
     * A construct requires a different type than it was provided with.
     */
    public static class RequiresDifferentType extends ErrorMessage {
        public RequiresDifferentType(String what, Position pos, Type expectedType, Type actualType) {
            super(String.format("%s requires a different type.", what), pos, expectedType, actualType);
        }
    }

    /**
     * The declaration and expression of a local block statement have different types.
     */
    public static class InitializerTypeMismatch extends ErrorMessage {
        public InitializerTypeMismatch(VariableDeclaration declaration, Type expressionType) {
            super(String.format("The type of the expression used to initialize the local variable '%s' does not match the type of the variable itself.",
                    declaration.name), declaration.position, declaration.type, expressionType);
        }
    }

    /**
     * The two declarations of a local block statement have different names.
     */
    public static class LocalNameMismatch extends ErrorMessage {
        public LocalNameMismatch(VariableDeclaration local, VariableDeclaration delocal) {
            super(String.format("Local variable '%s' has different names in local and delocal declaration.", local.name),
                    delocal.position, local.name, delocal.name);
        }
    }

    /**
     * The two declarations of a local block statement have different types.
     */
    public static class LocalTypeMismatch extends ErrorMessage {
        public LocalTypeMismatch(VariableDeclaration local, VariableDeclaration delocal) {
            super(String.format("Local variable '%s' has different types in local and delocal declaration.", local.name),
                    delocal.position, local.type, delocal.type);
        }
    }

    /**
     * An undefined symbol has been encountered.
     */
    public static class Undefined extends ErrorMessage {
        public Undefined(String what, Identifier name) {
            this(what, name.name, name.position);
        }

        public Undefined(String what, String name, Position position) {
            super(String.format("There is no %s defined with name '%s'.", what, name), position);
        }
    }

    /**
     * A variable has been indexed but is not an array.
     */
    public static class IndexingNonArray extends ErrorMessage {
        public IndexingNonArray(Identifier variable, Type variableType) {
            super(String.format("The indexing operation requires '%s' to be an array.", variable), variable.position,
                    "int[]", variableType);
        }
    }

    /**
     * A variable has been indexed but the index is not an integer.
     */
    public static class IndexingWithNonInteger extends ErrorMessage {
        public IndexingWithNonInteger(Position pos, Type indexType) {
            super("The index is required to be an integer.", pos,
                    PrimitiveType.IntType, indexType);
        }
    }

    /**
     * The binary operator of an expression requires different types than it was provided with.
     */
    public static class OperatorTypeMismatch extends ErrorMessage {
        public OperatorTypeMismatch(Expression operand,
                                    Type expectedOperandType, Type actualOperandType) {
            super("Binary expression requires operands of different types.", operand.position,
                    expectedOperandType, actualOperandType);
        }
    }

    /**
     * Two identifier generate an illegal alias for the same thing.
     */
    public static class AliasingIsNotAllowed extends ErrorMessage {
        public AliasingIsNotAllowed(Identifier i1, Identifier i2) {
            super(String.format("Identifiers '%s' and '%s' are aliases, which is not allowed.", i1, i2), i2.position);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Errors defined by various backends
    //////////////////////////////////////////////////////////////////////////

    public static class RequiredDisabledOptimization extends ErrorMessage {
        public RequiredDisabledOptimization(AvailableOptimization optimization,
                                            AvailableOptimization requiredBy) {
            super(String.format("Optimization %s is required by %s, but was manually disabled.",
                    optimization, requiredBy), null);
        }
    }

    /**
     * An entry point was defined by a user, but it is not present in the program.
     */
    public static class UnknownEntryPoint extends ErrorMessage {
        public UnknownEntryPoint(String entry) {
            super("Entry point '" + entry + "' is not defined!", null);
        }
    }

    /**
     * Many entry points are specified. But a maximum of 1 is allowed.
     */
    public static class MoreEntryPoints extends ErrorMessage {
        public MoreEntryPoints() {
            super("The program does specify more than one entry point.", null);
        }
    }

    public static class IllegalString extends ErrorMessage {
        public IllegalString(String message, Position position) {
            super(message, position);
        }
    }

    /**
     * A block does not have an exit point.
     */
    public static class UnclosedBlock extends ErrorMessage {
        public UnclosedBlock(ControlInstruction instruction) {
            super(String.format("Exit instruction for basic block with label(s) %s is missing.",
                    instruction.associatedLabels()), instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * An Annotation is missing required fields.
     */
    public static class MalformedAnnotation extends ErrorMessage {
        public MalformedAnnotation(Position pos, String message) {
            super(message, pos);
        }
    }

    /**
     * Reports the wrong arrow used for an entry or exit point.
     */
    public static class WrongArrow extends SyntaxError {
        public WrongArrow(Position pos, String constructName, String expectedArrow) {
            super(pos, "Syntax error detected. The wrong type of arrow is used for " + constructName + ".",
                    List.of(expectedArrow));
        }
    }

    /**
     * A procedure does not have an end instruction.
     */
    public static class UnclosedProcedure extends ErrorMessage {
        public UnclosedProcedure(BeginInstruction instruction) {
            super(String.format("End instruction for subroutine '%s' is missing.",
                    instruction.label), instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * An {@link Instruction} was found that does not belong to any block.
     */
    public static class InstructionOutsideOfBlock extends ErrorMessage {
        public InstructionOutsideOfBlock(Instruction instruction) {
            super("Instruction placed out of a block.", instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * An {@link Instruction} was found that does not belong to any procedure.
     */
    public static class InstructionOutsideOfProcedure extends ErrorMessage {
        public InstructionOutsideOfProcedure(Instruction instruction) {
            super("Instruction placed out of a subroutine.", instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * The label used in begin and end of a procedure are not the same.
     */
    public static class SubroutineMismatch extends ErrorMessage {
        public SubroutineMismatch(String beginLabel, String endLabel, Instruction instruction) {
            super("Labels of subroutine mismatch.", instruction.getSourcePosition().orElse(null), beginLabel, endLabel);
        }
    }

    /**
     * A conditional entry or exit refers to the same labels on both sides of the parameter list.
     * This is guaranteed to fail.
     */
    public static class DuplicateLabel extends ErrorMessage {
        public DuplicateLabel(Position position, String label) {
            super("Control instruction cannot refer to label '" + label + "' on both sides.", position);
        }
    }

    /**
     * The parameter lists of both definitions of a label are not of equal length.
     */
    public static class LabelParameterListSizeMismatch extends ErrorMessage {
        public LabelParameterListSizeMismatch(String label, int expectedArgumentCount, int providedArgumentCount, Position position) {
            super(String.format("The number of atoms in the parameter lists for '%s' do not match.", label), position,
                    expectedArgumentCount, providedArgumentCount);
        }
    }

    /**
     * A {@link Variable} that is not destroyed at the end of a block is invalid.
     */
    public static class UndestroyedVariables extends ErrorMessage {
        public UndestroyedVariables(ControlInstruction exitPoint, Set<Variable> undestroyed) {
            super("Not all variables are destroyed at the end of a block.", exitPoint.getSourcePosition().orElse(null),
                    "No variables", undestroyed);
        }
    }

    /**
     * An already initialized {@link Variable} is initialized again.
     */
    public static class VariableRedeclaration extends ErrorMessage {
        public VariableRedeclaration(Variable variable, Instruction instruction) {
            super(String.format("Redeclaration of variable '%s'.", variable.getName()),
                    instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * A {@link Variable} used was either already finalized or never initialized.
     */
    public static class UnknownVariable extends ErrorMessage {
        public UnknownVariable(Variable variable, Instruction instruction) {
            super(String.format("Usage of unknown or destroyed variable '%s'.", variable.getName()),
                    instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * A {@link Variable} was declared multiple times within a same procedure but outside of the
     * same {@link rc3.rssa.blocks.BasicBlock}. While it is unambiguous which {@link Variable} is
     * referred to, this conflicts with the SSA form of a program.
     */
    public static class NotInSSA extends ErrorMessage {
        public NotInSSA(String procedure, Variable variable, Instruction instruction) {
            super(String.format("The program is not in SSA form. Variable '%s' in procedure '%s' has multiple assignments.", variable, procedure),
                    instruction.getSourcePosition().orElse(null));
        }
    }

    /**
     * The amount of arguments passed with a {@link CallInstruction} does not match the called procedure.
     */
    public static class CallInputArgumentMismatch extends ErrorMessage {
        public CallInputArgumentMismatch(CallInstruction callInstruction, int expected) {
            super(String.format("Too %s input arguments for call to subroutine '%s'.",
                            expected < callInstruction.inputList.size() ? "many" : "few", callInstruction.procedure),
                    callInstruction.getSourcePosition().orElse(null), expected, callInstruction.inputList.size());
        }
    }

    /**
     * The amount of arguments received from a {@link CallInstruction} does not match the returning procedure.
     */
    public static class CallOutputArgumentMismatch extends ErrorMessage {
        public CallOutputArgumentMismatch(CallInstruction callInstruction, int expected) {
            super(String.format("Too %s output arguments for call to subroutine '%s'.",
                            expected < callInstruction.outputList.size() ? "many" : "few", callInstruction.procedure),
                    callInstruction.getSourcePosition().orElse(null), expected, callInstruction.outputList.size());
        }
    }
}
