package rc3.januscompiler.pass;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.table.Environment;
import rc3.lib.messages.ErrorMessage;

/**
 * This interface implements an analysis {@link Pass} in the compiler.
 * <p>
 * All implemented passes are executed in the order they are registered in the {@link Compiler} instance.
 * </p>
 */
public interface Pass {

    /**
     * Executes this pass and performs the implemented analysis.
     *
     * @param compiler    The {@link Compiler} executing this {@link Pass}.
     * @param environment The global {@link Environment} of the analysed {@link Program}.
     * @param program     The {@link Program} to analyse.
     * @throws ErrorMessage if a semantic error is detected during analysis.
     */
    void execute(Compiler compiler, Environment environment, Program program) throws ErrorMessage;

}
