package rc3.januscompiler.backend;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.pass.Pass;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.table.ProcedureEntry;
import rc3.januscompiler.types.Type;

import java.util.List;

/**
 * Calculates the required memory space to store all variables used within a procedure.
 * <p>
 * This abstract class provides the required traversal of the abstract syntax tree
 * to calculate all necessary sizes.
 * </p>
 * <p>
 * The first step is to calculate the offsets for every parameter. The initial offset
 * is determined by {@link VariableAllocator#initialParameterOffset(ProcedureDeclaration)}.
 * For the first parameter this initial offset and the parameter itself are passed to the
 * {@link VariableAllocator#applyParameterOffset(int, VariableDeclaration)} method, where
 * the offset is set and the offset used for the next parameter is returned.<br/>
 * This allows to implement many different stack layouts using this abstract class.<br/>
 * The offsets calculated for parameters are copied to the corresponding {@link ParameterType}s.<br/>
 * {@link ProcedureEntry#parameterAreaSize} is set to the value of {@link VariableAllocator#sizeOfAddress()}
 * times the amount of parameters.
 * </p>
 * <p>
 * The second step is executed for every {@link ProcedureDeclaration} directly after the first step.
 * This time the offsets for all local variables are calculated. Similarly to the first step, the initial
 * variable offset is returned by {@link VariableAllocator#initialLocalVariableOffset(ProcedureDeclaration)}.
 * Following occurrences of {@link VariableDeclaration}s within a procedure's body get their offset set by
 * a call to {@link VariableAllocator#applyVariableOffset(int, VariableDeclaration)}. The result of this
 * method is used as the offset for the next nested {@link VariableDeclaration}. <br/>
 * Since neighboured variable declarations within a scope are never active at the same time, they can
 * share the required space.
 * <pre>
 * procedure f()
 *  local   int i = 0
 *  delocal int i = 0
 *
 *  local   int a = 1
 *  local   int b = 2
 *  delocal int b = 2
 *  delocal int a = 1
 * </pre>
 * In this example, the required space is not <code>3 * sizeOf(IntType)</code> but rather
 * <code>2 * sizeOf(IntType)</code> since the local variable <code>i</code> may share the
 * memory location with <code>a</code>.<br/>
 * This behavior is implemented within this abstract class' allocation methods.<br/>
 * The required {@link ProcedureEntry#localVariableAreaSize} is equal to the highest
 * nested memory count.
 * </p>
 * <p>
 * In an additional pass the {@link ProcedureEntry#argumentAreaSize} is calculated. This size
 * comes from the procedure calls within a procedure and calculates the required memory space
 * for arguments. The value of {@link ProcedureEntry#argumentAreaSize} is equal to the highest
 * required {@link ProcedureEntry#parameterAreaSize} of all sub-calls.<br/>
 * If no other procedures are called, the value {@link ProcedureEntry#NO_PROCEDURES_CALLED} is
 * set instead.
 * </p>
 * <p>
 * If {@link Compiler.DumpOptions#dumpAllocation} is <code>true</code> in
 * {@link Compiler#dumpOptions}  the result of allocation for every {@link ProcedureDeclaration}
 * is printed to {@link System#out}.
 * </p>
 */
public abstract class VariableAllocator implements Pass {

    /**
     * Prints the result of the allocation process to the output.
     *
     * @param declaration The declaration to print the result of.
     */
    private static void showAllocationStats(ProcedureDeclaration declaration) {
        System.out.println();
        System.out.printf("Result of Variable Allocation for '%s':\n", declaration.name);
        System.out.printf("Required space for Parameters: %d\n", declaration.entry.parameterAreaSize);
        System.out.printf("Required space for Local Variables: %d\n", declaration.entry.localVariableAreaSize);
        System.out.printf("Required space for Procedure Calls: %d\n", declaration.entry.argumentAreaSize);
    }

    @Override
    public void execute(Compiler compiler, Environment environment, Program program) {
        // Allocate the builtin procedures.
        for (Builtins.Procedure procedure : Builtins.Procedure.values()) {
            var builtinEntry = environment.getProcedure(procedure.getName()).orElseThrow();
            allocateBuiltinProcedureOffsets(procedure, builtinEntry.parameterTypes);
            builtinEntry.parameterAreaSize = builtinEntry.parameterTypes.stream()
                    .mapToInt(parameterType -> sizeOf(parameterType.type)).sum();
        }

        // Allocate parameter and variable memory for all declarations.
        for (ProcedureDeclaration procedureDeclaration : program.procedures) {
            allocateParameters(procedureDeclaration);
            allocateLocalVariables(procedureDeclaration);
        }

        // Calculate the argument area size. This requires information from the earlier allocations.
        ArgumentAreaSizeVisitor argumentAreaSizeVisitor = new ArgumentAreaSizeVisitor(environment);
        for (ProcedureDeclaration procedureDeclaration : program.procedures) {
            procedureDeclaration.entry.argumentAreaSize = argumentAreaSizeVisitor.apply(procedureDeclaration);
        }

        // Show result of allocation if requested.
        if (compiler.dumpOptions.dumpAllocation) {
            program.procedures.forEach(VariableAllocator::showAllocationStats);
        }
    }

    /**
     * Used to allocate the offsets for the parameters of builtin procedures.
     * <p>
     * An additional method is required for this step, since the builtin procedures don't have
     * a declaration that could be used to allocate offsets.
     * </p>
     *
     * @param builtinProcedure The builtin to calculate offsets for.
     * @param params           The list of {@link ParameterType} where the offsets should be entered.
     */
    public abstract void allocateBuiltinProcedureOffsets(Builtins.Procedure builtinProcedure, List<ParameterType> params);

    /**
     * Calculates the initial offset to be used for the first parameter.
     *
     * @param procedureDeclaration The declaration of which the parameters are to be allocated.
     * @return The initial offset to be used for the first parameter.
     */
    public abstract int initialParameterOffset(ProcedureDeclaration procedureDeclaration);

    /**
     * Applies the current calculated parameter offset to a declared parameter.
     * <p>
     * This method returns the offset for the next parameter, which should be
     * equal to <code>currentOffset + sizeOfAddress()</code> in most cases.
     * </p>
     * <p>
     * The implementation of this method is allowed to let offsets grow in positive direction
     * as well as negative direction. Incrementing the offset and setting the value or setting
     * the value first before incrementing is also of free choice.
     * </p>
     *
     * @param currentOffset The current calculated offset.
     * @param parameter     The current declared parameter, whose offset has to be set.
     * @return The offset for the next parameter.
     */
    public abstract int applyParameterOffset(int currentOffset, VariableDeclaration parameter);

    /**
     * Allocates all parameter offsets and {@link ProcedureEntry#parameterAreaSize} for a
     * given {@link ProcedureDeclaration}.
     */
    public void allocateParameters(ProcedureDeclaration declaration) {
        int parameterOffset = initialParameterOffset(declaration);
        int requiredParameterArea = 0;

        for (int parameterIndex = 0; parameterIndex < declaration.parameters.size(); parameterIndex++) {
            var currentParameter = declaration.parameters.get(parameterIndex);
            var currentParameterType = declaration.entry.parameterTypes.get(parameterIndex);

            // Apply the offset and mirror the value to the corresponding parameter type.
            parameterOffset = applyParameterOffset(parameterOffset, currentParameter);
            currentParameterType.offset = currentParameter.entry.offset;

            // Keep track of the required space.
            requiredParameterArea += sizeOfAddress();
        }

        declaration.entry.parameterAreaSize = requiredParameterArea;
    }


    /**
     * Calculates the initial offset to be used for the first locally declared variable.
     *
     * @param procedureDeclaration The declaration of which the local variables are to be allocated.
     * @return The initial offset used for the first local variable.
     */
    public abstract int initialLocalVariableOffset(ProcedureDeclaration procedureDeclaration);


    /**
     * Applies the current calculated offset to a declared variable.
     * <p>
     * The returned value is used as the offset for the next nested locally declared variable and
     * should be equal to <code>currentOffset + sizeOf(variable.type)</code> in most cases.
     * </p>
     * <p>
     * The implementation of this method is allowed to let offsets grow in positive direction
     * as well as negative direction. Incrementing the offset and setting the value or setting
     * the value first before incrementing is also of free choice.
     * </p>
     *
     * @param currentOffset The current calculated offset.
     * @param variable      The current locally declared variable, whose offset has to be set.
     * @return The offset for the next nested locally declared variable.
     */
    public abstract int applyVariableOffset(int currentOffset, VariableDeclaration variable);

    /**
     * Allocates the offset for all locally declared variables as well as the
     * {@link ProcedureEntry#localVariableAreaSize} for a given {@link ProcedureDeclaration}.
     */
    public void allocateLocalVariables(ProcedureDeclaration declaration) {
        int variableOffset = initialLocalVariableOffset(declaration);

        declaration.entry.localVariableAreaSize = new LocalVariableAllocator(variableOffset).apply(declaration);
    }


    /**
     * Calculates the size in memory required for a variable of a given {@link Type}.
     *
     * @param type The {@link Type} of which the size should be calculated.
     * @return The size of the given type.
     */
    public abstract int sizeOf(Type type);

    /**
     * Calculates the size in memory required for an address.
     *
     * @return The size of an address.
     */
    public abstract int sizeOfAddress();


    /**
     * This {@link TreeVisitor} is used to
     * populate the offsets of locally declared variables and calculate the required
     * size to hold all variables of a procedure.
     */
    private class LocalVariableAllocator extends DoNothingTreeVisitor<Integer> {
        private final int previousOffset;

        public LocalVariableAllocator(int previousOffset) {
            super(0);
            this.previousOffset = previousOffset;
        }

        /**
         * Calculates the required size for variables within a list of statements.
         * <p>
         * The returned value is the maximum of all sizes for the statements in this list
         * since all variables declared within those statements are parallel to each other
         * and thus can share the memory.
         * </p>
         */
        public int requiredVariableAreaSize(List<Statement> statements) {
            return statements.stream()
                    .mapToInt(this::apply)
                    .max().orElse(0);
        }

        @Override
        public Integer visit(ProcedureDeclaration procedureDeclaration) {
            return requiredVariableAreaSize(procedureDeclaration.body);
        }

        @Override
        public Integer visit(MainProcedureDeclaration mainProcedure) {
            int variableOffset = 0;
            int requiredVariableArea = 0;

            // First populate the declaration list of the main procedure before processing the body.
            for (int variableIndex = 0; variableIndex < mainProcedure.variables.size(); variableIndex++) {
                var currentVariable = mainProcedure.variables.get(variableIndex);

                variableOffset = applyVariableOffset(variableOffset, currentVariable);
                requiredVariableArea += sizeOf(currentVariable.type);
            }

            var localAllocator = new LocalVariableAllocator(variableOffset);
            return requiredVariableArea + localAllocator.requiredVariableAreaSize(mainProcedure.body);
        }


        @Override
        public Integer visit(LocalBlockStatement localBlockStatement) {
            int nextOffset = applyVariableOffset(previousOffset, localBlockStatement.localDeclaration);

            // Populate the children of this declaration using the new offset.
            var localAllocator = new LocalVariableAllocator(nextOffset);
            int requiredBodySize = localAllocator.requiredVariableAreaSize(localBlockStatement.body);

            return requiredBodySize + sizeOf(localBlockStatement.localDeclaration.type);
        }

        @Override
        public Integer visit(IfStatement ifStatement) {
            return Math.max(
                    requiredVariableAreaSize(ifStatement.thenStatements),
                    requiredVariableAreaSize(ifStatement.elseStatements));
        }

        @Override
        public Integer visit(LoopStatement loopStatement) {
            return Math.max(
                    requiredVariableAreaSize(loopStatement.doStatements),
                    requiredVariableAreaSize(loopStatement.loopStatements));
        }
    }


    /**
     * This {@link TreeVisitor} calculates
     * the required size for arguments of a given {@link ProcedureDeclaration}.
     */
    private static class ArgumentAreaSizeVisitor extends DoNothingTreeVisitor<Integer> {
        private final Environment globalEnvironment;

        public ArgumentAreaSizeVisitor(Environment globalEnvironment) {
            super(ProcedureEntry.NO_PROCEDURES_CALLED);
            this.globalEnvironment = globalEnvironment;
        }

        /**
         * Calculates the required argument area size of a list of statements.
         * <p>
         * This is equal to the highest argument area size of any statement within the list.
         * </p>
         */
        public int requiredArgumentAreaSize(List<Statement> statements) {
            return statements.stream()
                    .mapToInt(this::apply)
                    .max().orElse(0);
        }

        @Override
        public Integer visit(ProcedureDeclaration procedureDeclaration) {
            return requiredArgumentAreaSize(procedureDeclaration.body);
        }

        @Override
        public Integer visit(MainProcedureDeclaration mainProcedure) {
            return requiredArgumentAreaSize(mainProcedure.body);
        }

        @Override
        public Integer visit(BuiltinStatement builtinStatement) {
            ProcedureEntry builtinCalledProcedure = globalEnvironment
                    .getProcedure(builtinStatement.builtinProcedure.getName())
                    .orElseThrow(); // The procedure should always be present.

            return builtinCalledProcedure.parameterAreaSize;
        }

        @Override
        public Integer visit(CallStatement callStatement) {
            ProcedureEntry calledProcedure = globalEnvironment.getProcedure(callStatement.procedureName)
                    .orElseThrow(); // The procedure should always be present.

            return calledProcedure.parameterAreaSize;
        }

        @Override
        public Integer visit(LocalBlockStatement localBlockStatement) {
            return requiredArgumentAreaSize(localBlockStatement.body);
        }

        @Override
        public Integer visit(IfStatement ifStatement) {
            return Math.max(
                    requiredArgumentAreaSize(ifStatement.thenStatements),
                    requiredArgumentAreaSize(ifStatement.elseStatements));
        }

        @Override
        public Integer visit(LoopStatement loopStatement) {
            return Math.max(
                    requiredArgumentAreaSize(loopStatement.doStatements),
                    requiredArgumentAreaSize(loopStatement.loopStatements));
        }
    }
}
