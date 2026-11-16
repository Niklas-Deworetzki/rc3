package rc3.januscompiler.pass;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.*;
import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;
import rc3.januscompiler.types.Type;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.parsing.Position;

import java.util.Iterator;
import java.util.List;

/**
 * This analysis {@link Pass} analyses the procedure bodies of Janus program.
 * <p>
 * After this analysis {@link Pass} it is guaranteed that every {@link Declaration} is decorated with a
 * corresponding {@link Entry} instance and that the Janus types for every operation are correct.
 * </p>
 */
public final class LocalAnalysisPass implements Pass {

    @Override
    public void execute(Compiler compiler, Environment environment, Program program) {
        TypeCheckingVisitor checkTypes = new TypeCheckingVisitor(environment);
        program.procedures.forEach(checkTypes);
    }

    /**
     * Creates a {@link VariableEntry} from a {@link VariableDeclaration}.
     */
    private static VariableEntry createVariableEntry(VariableDeclaration declaration, boolean isParameter) {
        VariableEntry resultEntry = new VariableEntry(declaration.name, declaration.type, isParameter);
        declaration.entry = resultEntry;
        return resultEntry;
    }

    /**
     * The {@link TreeVisitor} checking the {@link Type} of expressions
     * and variables.
     */
    private static class TypeCheckingVisitor extends DoNothingVoidTreeVisitor {
        private final Environment environment;

        public TypeCheckingVisitor(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void visitVoid(ProcedureDeclaration procedureDeclaration) {
            for (var parameter : procedureDeclaration.parameters) {
                environment.enter(createVariableEntry(parameter, true));
            }

            procedureDeclaration.body.forEach(this);
            environment.clearLocalVariables();
        }

        @Override
        public void visitVoid(MainProcedureDeclaration mainProcedure) {
            for (var variable : mainProcedure.variables) {
                environment.enter(createVariableEntry(variable, false));
            }

            mainProcedure.body.forEach(this);
            environment.clearLocalVariables();
        }


        private void typeCheckArgumentList(Position reportPosition, ProcedureEntry procedureEntry, List<Variable> argumentList) {
            Iterator<ParameterType> parameterTypes = procedureEntry.parameterTypes.iterator();
            Iterator<Variable> arguments = argumentList.iterator();
            int argumentNumber = 1;

            while (parameterTypes.hasNext() && arguments.hasNext()) {
                Type parameterType = parameterTypes.next().type;
                Type argumentType = arguments.next().getDataType(environment);

                if (!argumentType.equals(parameterType)) {
                    throw new ErrorMessage.ParameterTypeMismatch(procedureEntry.getName(), argumentNumber,
                            parameterType, argumentType, argumentList.get(argumentNumber - 1).position);
                }

                argumentNumber++;
            }

            // More passed arguments than parameters.
            if (arguments.hasNext()) {
                throw new ErrorMessage.TooManyArguments(procedureEntry.getName(),
                        procedureEntry.parameterTypes.size(), argumentList.size(), reportPosition);
            }
            // More defined parameters than arguments.
            if (parameterTypes.hasNext()) {
                throw new ErrorMessage.NotEnoughArguments(procedureEntry.getName(),
                        procedureEntry.parameterTypes.size(), argumentList.size(), reportPosition);
            }
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            Type lhsType = assignStatement.variable.getDataType(environment);
            Type rhsType = assignStatement.value.getDataType(environment);

            if (lhsType != rhsType) {
                throw new ErrorMessage.CombinesDifferentTypes("Assignment", assignStatement.position,
                        lhsType, rhsType);
            }
            if (lhsType != PrimitiveType.IntType) {
                throw new ErrorMessage.RequiresDifferentType("Assignment", assignStatement.position,
                        PrimitiveType.IntType, lhsType);
            }
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            ProcedureEntry builtinEntry = environment.getProcedure(builtinStatement.builtinProcedure.getName())
                    .orElseThrow(); // This should always be present.

            typeCheckArgumentList(builtinStatement.position, builtinEntry, builtinStatement.argumentList);
        }

        @Override
        public void visitVoid(CallStatement callStatement) {
            ProcedureEntry procedureEntry = environment.getProcedure(callStatement.procedureName)
                    .orElseThrow(() -> new ErrorMessage.Undefined("Procedure", callStatement.procedureName));

            typeCheckArgumentList(callStatement.position, procedureEntry, callStatement.argumentList);
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            Type conditionType = ifStatement.condition.getDataType(environment);
            if (conditionType != PrimitiveType.BoolType) {
                throw new ErrorMessage.RequiresDifferentType("Condition", ifStatement.condition.position,
                        PrimitiveType.BoolType, conditionType);
            }

            Type assertionType = ifStatement.assertion.getDataType(environment);
            if (assertionType != PrimitiveType.BoolType) {
                throw new ErrorMessage.RequiresDifferentType("Condition", ifStatement.assertion.position,
                        PrimitiveType.BoolType, assertionType);
            }

            ifStatement.thenStatements.forEach(this);
            ifStatement.elseStatements.forEach(this);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            var localDeclaration = localBlockStatement.localDeclaration;
            var delocalDeclaration = localBlockStatement.delocalDeclaration;

            if (localDeclaration.type instanceof ArrayType) {
                throw new ErrorMessage.TypeRestrictions(localDeclaration.type, "local block declaration",
                        localDeclaration.position);
            }
            if (!localDeclaration.name.equals(delocalDeclaration.name)) {
                throw new ErrorMessage.LocalNameMismatch(localDeclaration, delocalDeclaration);
            }
            if (!localDeclaration.type.equals(delocalDeclaration.type)) {
                throw new ErrorMessage.LocalTypeMismatch(localDeclaration, delocalDeclaration);
            }

            // Local declaration and de-local declaration share one entry.
            VariableEntry localEntry = createVariableEntry(localDeclaration, false);
            delocalDeclaration.entry = localEntry;

            if (localBlockStatement.localInitializer.getDataType(environment) != localEntry.type) {
                throw new ErrorMessage.InitializerTypeMismatch(localDeclaration,
                        localBlockStatement.localInitializer.dataType);
            }
            if (localBlockStatement.delocalInitializer.getDataType(environment) != localEntry.type) {
                throw new ErrorMessage.InitializerTypeMismatch(delocalDeclaration,
                        localBlockStatement.delocalInitializer.dataType);
            }

            environment.enter(localEntry);
            localBlockStatement.body.forEach(this);
            environment.exit(localEntry);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            Type fromConditionType = loopStatement.fromCondition.getDataType(environment);
            if (fromConditionType != PrimitiveType.BoolType) {
                throw new ErrorMessage.RequiresDifferentType("Condition", loopStatement.fromCondition.position,
                        PrimitiveType.BoolType, fromConditionType);
            }

            Type untilConditionType = loopStatement.untilCondition.getDataType(environment);
            if (untilConditionType != PrimitiveType.BoolType) {
                throw new ErrorMessage.RequiresDifferentType("Condition", loopStatement.untilCondition.position,
                        PrimitiveType.BoolType, untilConditionType);
            }

            loopStatement.doStatements.forEach(this);
            loopStatement.loopStatements.forEach(this);
        }

        @Override
        public void visitVoid(SkipStatement skipStatement) {
            // Nothing to do
        }

        @Override
        public void visitVoid(SwapStatement swapStatement) {
            Type lhstype = swapStatement.lhsvar.getDataType(environment);
            Type rhstype = swapStatement.rhsvar.getDataType(environment);

            if (lhstype != rhstype) {
                throw new ErrorMessage.CombinesDifferentTypes("Swap statement", swapStatement.position,
                        lhstype, rhstype);
            }

            /* Int is required, since Swap is just a short form of writing the following:
             *  a ^= b
             *  b ^= a
             *  a ^= b
             * Which requires integers for the bitwise operation.
             */
            if (lhstype != PrimitiveType.IntType) {
                throw new ErrorMessage.RequiresDifferentType("Swap statement", swapStatement.position,
                        PrimitiveType.IntType, lhstype);
            }
        }
    }
}
