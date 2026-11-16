package rc3.januscompiler.pass;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.syntax.*;
import rc3.januscompiler.syntax.visitor.DoNothingTreeVisitor;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.januscompiler.table.*;
import rc3.januscompiler.types.Type;
import rc3.lib.messages.ErrorMessage;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * This analysis {@link Pass} analyses global symbols and generates {@link Entry Entries}
 * for the global {@link Environment}.
 * <p>
 * After this analysis {@link Pass} is completed, the global {@link Environment} will be populated with all
 * global procedures and their parameters. It is also ensured that procedures and parameters can be resolved
 * by making sure, the procedure and parameter names are unique in their respective scope.
 * </p>
 * <p>
 * This {@link Pass} also ensures that a 'main' procedure is present.
 * </p>
 */
public final class GlobalAnalysisPass implements Pass {

    @Override
    public void execute(Compiler compiler, Environment environment, Program program) {
        enterBuiltinProcedures(environment);

        EnvironmentBuilderVisitor builderVisitor = new EnvironmentBuilderVisitor();
        for (var declaration : program.procedures) {
            ProcedureEntry entry = declaration.accept(builderVisitor);

            if (!environment.enterGlobal(entry)) {
                // The entry cannot be added, since there already exists an entry with the same name.
                throw new ErrorMessage.RedeclarationAs("procedure", declaration);
            }
        }

        // Check if a main procedure is present.
        Optional<ProcedureEntry> mainEntry = environment.getProcedure(new Identifier("main"));
        if (!mainEntry.isPresent()) {
            throw new ErrorMessage.MissingMainProcedure();
        }
    }

    private static void enterBuiltinProcedures(Environment environment) {
        for (Builtins.Procedure procedure : Builtins.Procedure.values()) {
            var builtinEntry = new ProcedureEntry(procedure.getName(), procedure.getRequiredParameterTypes());
            environment.enterGlobal(builtinEntry);
        }
    }

    /**
     * The {@link TreeVisitor} used to create
     * {@link ProcedureEntry ProcedureEntries}
     */
    private static class EnvironmentBuilderVisitor extends DoNothingTreeVisitor<ProcedureEntry> {

        @Override
        public ProcedureEntry visit(ProcedureDeclaration procedureDeclaration) {
            Optional<VariableDeclaration> duplicateName = findDuplicateNames(procedureDeclaration.parameters);
            if (duplicateName.isPresent()) {
                throw new ErrorMessage.RedeclarationAs("parameter", duplicateName.get());
            }

            List<Type> parameterTypes = new ArrayList<>();

            for (var parameter : procedureDeclaration.parameters) {
                parameterTypes.add(parameter.type);
            }

            ProcedureEntry resultEntry = new ProcedureEntry(procedureDeclaration.name, parameterTypes);
            procedureDeclaration.entry = resultEntry;
            return resultEntry;
        }

        @Override
        public ProcedureEntry visit(MainProcedureDeclaration mainProcedure) {
            Optional<VariableDeclaration> duplicateName = findDuplicateNames(mainProcedure.variables);
            if (duplicateName.isPresent()) {
                throw new ErrorMessage.RedeclarationAs("variable", duplicateName.get());
            }

            ProcedureEntry resultEntry = new ProcedureEntry(mainProcedure.name, emptyList());
            mainProcedure.entry = resultEntry;
            return resultEntry;
        }
    }


    private static <D extends Declaration<?>> Optional<D> findDuplicateNames(List<D> declarations) {
        Set<Identifier> names = new HashSet<>();

        for (var declaration : declarations) {
            if (names.contains(declaration.name)) {
                return Optional.of(declaration);
            }
            names.add(declaration.name);
        }

        return Optional.empty();
    }
}
