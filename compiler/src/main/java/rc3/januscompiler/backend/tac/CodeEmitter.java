package rc3.januscompiler.backend.tac;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is an abstraction layer above the raw printing of {@link String Strings} to the output file
 * when generating C code from this compiler.
 */
public final class CodeEmitter {
    private final PrintStream out;

    public CodeEmitter(PrintStream printStream) {
        this.out = printStream;
    }

    /**
     * Returns the raw {@link OutputStream} that is written to.
     *
     * @return The raw {@link OutputStream} used for underlying writes.
     */
    public OutputStream getRawOutput() {
        return out;
    }

    /**
     * Writes a line break to the output stream.
     */
    public void emitNewLine() {
        out.println();
    }

    /**
     * Emits a generic text to the output stream. If any additional parameters are passed as varargs to this
     * method, the text is used as an format {@link String}.
     *
     * @param text       The text to print.
     * @param formatArgs Additional parameters used if the text is a format string.
     * @see String#format(String, Object...)
     */
    public void emit(String text, Object... formatArgs) {
        out.printf(text, formatArgs);
    }

    /**
     * Emits a generic text to the output stream and appends a newline to it. If any additional parameters are
     * passed as varargs to this method, the text is used as an format {@link String}.
     *
     * @param line       The text to print.
     * @param formatArgs Additional parameters used if the text is a format string.
     * @see String#format(String, Object...)
     */
    public void emitLine(String line, Object... formatArgs) {
        if (formatArgs.length == 0) {
            out.print(line);
        } else {
            out.printf(line, formatArgs);
        }
        emitNewLine();
    }

    /**
     * Emits the given text as part of a C comment. No linebreaks are printed unless they are part of the
     * given comment text. If additional parameters are passed as varargs to this method, the comment text
     * is used as an format {@link String}.
     *
     * @param comment    The comment text to print.
     * @param formatArgs Additional parameters used if the comment text is a format string.
     * @see String#format(String, Object...)
     */
    public void emitComment(String comment, Object... formatArgs) {
        String commentString = (formatArgs.length == 0)
                ? comment
                : String.format(comment, formatArgs);

        emitLine("/* %s */", commentString);
    }

    /**
     * Emits a declaration for the given {@link CVariable} and initializes the newly declared variable
     * using the given initial value. Declaration and initialization is done as a single statement that is
     * terminated by a semicolon.
     * A line break is inserted after the declaration.
     *
     * @param variable     The declared variable.
     * @param initialValue The initial value of the variable.
     */
    public void emitDeclaration(CVariable variable, String initialValue) {
        emitLine("\t%s = %s;", variable.getDeclarationString(), initialValue);
    }

    /**
     * Emits an assign statement. The result of applying the unary operator to the operand is
     * assigned to the given target.
     * <pre>
     *  {target} = {unary operator} {operand};
     * </pre>
     *
     * @param target        The target of the assignment.
     * @param unaryOperator The unary operator that the operand is applied to.
     * @param operand       The operand to apply to the unary operator.
     */
    public void emitAssignment(String target, String unaryOperator, String operand) {
        emitLine("\t%s = %s %s;", target, unaryOperator, operand);
    }

    /**
     * Emits an assign statement. The result of applying the binary operator to the both operands is
     * assigned to the given target.
     * <pre>
     *     {target} = {lhs} {binary operator} {rhs};
     * </pre>
     *
     * @param target         The target of the assignment.
     * @param lhs            The left-hand side operand.
     * @param binaryOperator The binary operator.
     * @param rhs            The right-hand side operand.
     */
    public void emitAssignment(String target, String lhs, String binaryOperator, String rhs) {
        emitLine("\t%s = %s %s %s;", target, lhs, binaryOperator, rhs);
    }

    /**
     * Emits an assign statement. The value of the variable is assigned to the given target.
     * <pre>
     *     {target} = {source};
     * </pre>
     *
     * @param target The target of the assignment.
     * @param source The source of the assignment.
     */
    public void emitAssignment(String target, String source) {
        emitLine("\t%s = %s;", target, source);
    }

    /**
     * Emits a call statement. The called c function is given as a name. The values passed as parameters
     * are given as a stream of values.
     * <pre>
     *     {name}( {arguments separated by comma} );
     * </pre>
     * <p>
     * Unlike the parameter list, the argument list can just be empty if no arguments are provided.
     * </p>
     *
     * @param procedureName The target of the call statement.
     * @param arguments     The arguments passed to the called function.
     */
    public void emitCall(String procedureName, Stream<String> arguments) {
        String argumentList = arguments.collect(Collectors.joining(", "));
        emitLine("\t%s(%s);", procedureName, argumentList);
    }

    /**
     * Emits a goto statement. The C standard requires that the target of this statement is
     * within the same function as this statement.
     * <pre>
     *     goto {label};
     * </pre>
     *
     * @param label The label jumped to by this statement.
     */
    public void emitGoto(String label) {
        emitLine("\tgoto %s;", label);
    }

    /**
     * Emits a condition that executed the following statement only if the condition evaluates to a non-zero
     * value at runtime.
     * <pre>
     *     if ({lhs} {relative operator} {rhs})
     * </pre>
     *
     * @param lhs              The left-hand side operand.
     * @param relativeOperator The operator used to compare both operands.
     * @param rhs              The right-hand side operand.
     */
    public void emitCondition(String lhs, String relativeOperator, String rhs) {
        emitLine("\tif (%s %s %s)", lhs, relativeOperator, rhs);
    }

    /**
     * Emits an assign statement. The value of a member is loaded from a pointer to a structure and
     * stored into the target.
     * <pre>
     *     {target} = {source variable}->{member name};
     * </pre>
     *
     * @param target The target of the assignment.
     * @param source The source of the assignment. This must be a pointer to a struct.
     * @param member The name of the struct member that should be loaded from the struct.
     */
    public void emitMemberLoad(String target, String source, String member) {
        emitLine("\t%s = %s%s;", target, source, member);
    }

    /**
     * Emits an assign statement. The offset is added to the source and the value at the
     * resulting address is loaded and assigned to the target.
     * <pre>
     *     {target} = *({source} + {offset});
     * </pre>
     *
     * @param target The target of the assignment.
     * @param source The source of the assignment. This must be a pointer.
     * @param offset The offset to add to the pointer before de-referencing it.
     */
    public void emitLoad(String target, String source, String offset) {
        emitLine("\t%s = *(%s + %s);", target, source, offset);
    }

    /**
     * Emits an assign statement. A value is loaded from the address stored in the source and assigned
     * to the target.
     * <pre>
     *     {target declaration} = *(source);
     * </pre>
     *
     * @param target The target of the assignment.
     * @param source The source of the assignment. This must be a pointer.
     */
    public void emitLoad(String target, String source) {
        emitLine("\t%s = *(%s);", target, source);
    }

    /**
     * Emits an assignment. The value of the source is stored at the member of the target variable.
     * <pre>
     *     {target}->{member name} = {source};
     * </pre>
     *
     * @param target The target of the assignment. This must be a pointer to a struct.
     * @param member The name of the struct member.
     * @param source The value to be stored in the struct member.
     */
    public void emitMemberStore(String target, String member, String source) {
        emitLine("\t%s%s = %s;", target, member, source);
    }

    /**
     * Emits an assignment. The value of the source is stored at the target, after the offset was added
     * to the target.
     * <pre>
     *     *({target} + {offset}) = {source};
     * </pre>
     *
     * @param target The target of the assignment. This must be a pointer.
     * @param offset The offset to add to the value of the target.
     * @param source The value to be stored at the address.
     */
    public void emitStore(String target, String offset, String source) {
        emitLine("\t*(%s + %s) = %s;", target, offset, source);
    }

    /**
     * Emits an assignment. The value of the source is stored at the target.
     * <pre>
     *     *({target}) = {source};
     * </pre>
     *
     * @param target The target of the assignment. This must be a pointer.
     * @param source The value to be stored at the address.
     */
    public void emitStore(String target, String source) {
        emitLine("\t*(%s) = %s;", target, source);
    }

    /**
     * Prints an instruction, that outputs the variable name and value at runtime.
     *
     * @param localVariable The variable that should be printed to the output at runtime.
     */
    public void printDeclaredVariable(CVariable localVariable, String variableName) {
        emitLine("\tprintVariable%s(\"%s\", %s);",
                localVariable.type().getName(), variableName, localVariable.name());
    }
}
