package rc3.januscompiler;

import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.PrimitiveType;

import java.util.stream.Collectors;

public final class JanusPrettyPrinter extends TreeVisitor<String> {
    public int indentationLevel = 2;

    @Override
    public String visit(AssignStatement assignStatement) {
        return String.format("%s %s= %s",
                assignStatement.variable.accept(this),
                assignStatement.modificationOperator,
                assignStatement.value.accept(this));
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        return String.format("(%s %s %s)",
                binaryExpression.lhs.accept(this),
                binaryExpression.operator,
                binaryExpression.rhs.accept(this));
    }

    @Override
    public String visit(BuiltinExpression builtinExpression) {
        return builtinExpression.expression.getName() +
                builtinExpression.argumentList.stream().map(this)
                        .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public String visit(BuiltinStatement builtinStatement) {
        return builtinStatement.builtinProcedure.getName() +
                builtinStatement.argumentList.stream().map(this)
                        .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public String visit(CallStatement callStatement) {
        return String.format("%s %s%s",
                callStatement.direction.asCallKeyword(),
                callStatement.procedureName,
                callStatement.argumentList.stream().map(this)
                        .collect(Collectors.joining(", ", "(", ")")));
    }

    @Override
    public String visit(IfStatement ifStatement) {
        return String.format("if %s then\n%selse\n%sfi %s",
                ifStatement.condition.accept(this),
                ifStatement.thenStatements.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel),
                ifStatement.elseStatements.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel),
                ifStatement.condition.accept(this));
    }

    @Override
    public String visit(IndexedVariable indexedVariable) {
        return String.format("%s[%s]", indexedVariable.name, indexedVariable.index.accept(this));
    }

    @Override
    public String visit(IntLiteral intLiteral) {
        return Integer.toString(intLiteral.value);
    }

    @Override
    public String visit(MainProcedureDeclaration mainProcedure) {
        final String variables = mainProcedure.variables.stream().map(this)
                .collect(Collectors.joining("\n"));
        final String statements = mainProcedure.body.stream().map(this)
                .collect(Collectors.joining("\n"));

        return String.format("procedure main()\n%s\n%s",
                variables.indent(indentationLevel),
                statements.indent(indentationLevel));
    }

    @Override
    public String visit(LocalBlockStatement localBlockStatement) {
        return String.format("local %s = %s\n%sdelocal %s = %s",
                localBlockStatement.localDeclaration.accept(this),
                localBlockStatement.localInitializer.accept(this),
                localBlockStatement.body.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel),
                localBlockStatement.delocalDeclaration.accept(this),
                localBlockStatement.delocalInitializer.accept(this));
    }

    @Override
    public String visit(NamedVariable namedVariable) {
        return namedVariable.name.toString();
    }

    @Override
    public String visit(ProcedureDeclaration procedureDeclaration) {
        return String.format("procedure %s(%s)\n%s",
                procedureDeclaration.name,
                procedureDeclaration.parameters.stream().map(this)
                        .collect(Collectors.joining(", ")),
                procedureDeclaration.body.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel));
    }

    @Override
    public String visit(Program program) {
        return program.procedures.stream().map(this)
                .collect(Collectors.joining("\n\n"))
                .indent(indentationLevel);
    }

    @Override
    public String visit(SkipStatement skipStatement) {
        return "skip";
    }

    @Override
    public String visit(StackNilLiteral stackNilLiteral) {
        return "nil";
    }

    @Override
    public String visit(SwapStatement swapStatement) {
        return String.format("%s <=> %s",
                swapStatement.lhsvar.accept(this),
                swapStatement.rhsvar.accept(this));
    }

    @Override
    public String visit(VariableDeclaration variableDeclaration) {
        final String declaredType = (variableDeclaration.type instanceof PrimitiveType primitiveType) ?
                primitiveType.toString() : "int";

        final String declaration = String.format("%s %s", declaredType, variableDeclaration.name);
        if (variableDeclaration.type instanceof ArrayType arrayType) {
            return String.format("%s[%d]", declaration, arrayType.size);
        }
        return declaration;
    }

    @Override
    public String visit(VariableExpression variableExpression) {
        return variableExpression.variable.accept(this);
    }

    @Override
    public String visit(LoopStatement loopStatement) {
        return String.format("from %s do\n%sloop\n%suntil %s",
                loopStatement.fromCondition.accept(this),
                loopStatement.doStatements.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel),
                loopStatement.loopStatements.stream().map(this)
                        .collect(Collectors.joining("\n"))
                        .indent(indentationLevel),
                loopStatement.untilCondition.accept(this));
    }
}
