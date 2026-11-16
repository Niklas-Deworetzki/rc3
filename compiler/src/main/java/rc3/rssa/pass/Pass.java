package rc3.rssa.pass;

import rc3.lib.messages.ErrorMessage;
import rc3.rssa.instances.Program;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class provides different interfaces for analysis in RSSA.
 */
public final class Pass {

    /**
     * A {@link Pass} that transforms some input state into an output state,
     * performing analysis and possibly detecting error conditions.
     */
    public interface Transformation<I, O> extends Function<I, O> {
        @Override
        default O apply(I i) {
            return execute(i);
        }

        /**
         * Executes this pass and performs the implemented analysis.
         *
         * @param input The data structure to analyse.
         * @return Result of analysis.
         * @throws ErrorMessage if a semantic error is detected during analysis.
         */
        O execute(I input) throws ErrorMessage;
    }

    /**
     * A {@link Pass} that validates some input state, performing analysis
     * and possibly detecting error conditions.
     */
    public interface Validation extends Consumer<Program> {
        @Override
        default void accept(Program program) {
            execute(program);
        }

        /**
         * Executes this pass and performs the implemented analysis.
         *
         * @param program The program structure to analyse.
         * @throws ErrorMessage if a semantic error is detected during analysis.
         */
        void execute(Program program) throws ErrorMessage;
    }

}
