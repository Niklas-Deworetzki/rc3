package rc3.januscompiler.pass;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingVoidTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.Identifier;
import rc3.lib.messages.ErrorMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This analysis {@link Pass} does compile-time checks to ensure the conservation of information in a
 * Janus {@link Program}.
 * <p>
 * In Janus it is forbidden to perform an assignment where the modifying variable is equal to the
 * modified one. Such an assignment would translate to xor-ing a variable with itself or potentially
 * subtracting the variable from itself. In both cases, the result would be zero and the information
 * earlier stored in the variable would be destroyed.
 * </p>
 * <p>
 * Additionally it is forbidden to pass the same variable as different names to a procedure. If a variable
 * would be passed with two different names, the two names could be used for an assignment, destroying information
 * without the compiler being able to prevent it.
 * </p>
 */
public final class AliasingAnalysisPass implements Pass {

    @Override
    public void execute(Compiler compiler, Environment environment, Program program) {
        AliasCheckingVisitor aliasCheckingVisitor = new AliasCheckingVisitor();

        for (ProcedureDeclaration procedure : program.procedures) {
            procedure.body.forEach(aliasCheckingVisitor);
        }
    }

    /**
     * The {@link TreeVisitor} performing the analysis.
     */
    private static class AliasCheckingVisitor extends DoNothingVoidTreeVisitor {
        // A set of forbidden names used to perform aliasing checks.
        private final Set<Identifier> forbiddenNames = new HashSet<>();

        public void checkForbiddenAlias(Identifier name) {
            for (Identifier forbidden : forbiddenNames) {
                if (forbidden.equals(name)) {
                    throw new ErrorMessage.AliasingIsNotAllowed(forbidden, name);
                }
            }
        }

        public void checkArgumentList(List<Variable> argumentList) {
            for (Variable argument : argumentList) {
                checkForbiddenAlias(argument.name);
                forbiddenNames.add(argument.name);

                if (argument instanceof IndexedVariable) {
                    ((IndexedVariable) argument).index.accept(this);
                }
            }

            // Remove all arguments from set of forbidden names.
            for (Variable argument : argumentList) {
                forbiddenNames.remove(argument.name);
            }
        }

        @Override
        public void visitVoid(AssignStatement assignStatement) {
            // A variable on the left-hand side must not occur in the expression on the right-hand side.
            forbiddenNames.add(assignStatement.variable.name);

            assignStatement.value.accept(this);

            // If the variable is an indexed variable, it must not appear in the expressions on either side.
            if (assignStatement.variable instanceof IndexedVariable) {
                ((IndexedVariable) assignStatement.variable).index.accept(this);
            }

            forbiddenNames.remove(assignStatement.variable.name);
        }

        @Override
        public void visitVoid(BinaryExpression binaryExpression) {
            binaryExpression.lhs.accept(this);
            binaryExpression.rhs.accept(this);
        }

        @Override
        public void visitVoid(BuiltinExpression builtinExpression) {
            checkArgumentList(builtinExpression.argumentList);
        }

        @Override
        public void visitVoid(BuiltinStatement builtinStatement) {
            checkArgumentList(builtinStatement.argumentList);
        }

        @Override
        public void visitVoid(CallStatement callStatement) {
            checkArgumentList(callStatement.argumentList);
        }

        @Override
        public void visitVoid(IfStatement ifStatement) {
            ifStatement.thenStatements.forEach(this);
            ifStatement.elseStatements.forEach(this);
        }

        @Override
        public void visitVoid(IndexedVariable indexedVariable) {
            checkForbiddenAlias(indexedVariable.name);
            indexedVariable.index.accept(this);
        }

        @Override
        public void visitVoid(LocalBlockStatement localBlockStatement) {
            // The declared name must not occur in the initializing and de-initializing expression.
            forbiddenNames.add(localBlockStatement.localDeclaration.name);
            {
                localBlockStatement.localInitializer.accept(this);
                localBlockStatement.delocalInitializer.accept(this);
            }
            forbiddenNames.remove(localBlockStatement.localDeclaration.name);

            // Finally the body of the statement
            localBlockStatement.body.forEach(this);
        }

        @Override
        public void visitVoid(NamedVariable namedVariable) {
            checkForbiddenAlias(namedVariable.name);
        }

        @Override
        public void visitVoid(SwapStatement swapStatement) {
            boolean lhsIsArray = swapStatement.lhsvar instanceof IndexedVariable;
            boolean rhsIsArray = swapStatement.rhsvar instanceof IndexedVariable;

            if (lhsIsArray) { // M[x] <=> x is not allowed.
                forbiddenNames.add(swapStatement.rhsvar.name);
                {
                    ((IndexedVariable) swapStatement.lhsvar).index.accept(this);
                }
                forbiddenNames.remove(swapStatement.rhsvar.name);
            }
            if (rhsIsArray) { // x <=> M[x] is not allowed.
                forbiddenNames.add(swapStatement.lhsvar.name);
                {
                    ((IndexedVariable) swapStatement.rhsvar).index.accept(this);
                }
                forbiddenNames.remove(swapStatement.lhsvar.name);
            }
        }

        @Override
        public void visitVoid(VariableExpression variableExpression) {
            variableExpression.variable.accept(this);
        }

        @Override
        public void visitVoid(LoopStatement loopStatement) {
            loopStatement.doStatements.forEach(this);
            loopStatement.loopStatements.forEach(this);
        }
    }
}
