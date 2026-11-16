package rc3.rssa.pass;

import rc3.lib.messages.ErrorMessage;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.DoNothingVoidRSSAVisitor;

import java.util.List;

/**
 * This pass is used to validate all {@link CallInstruction} within a {@link Program}.
 * <p>
 * If the target of a {@link CallInstruction} is unknown or the amount
 * of parameters is not correct, an {@link ErrorMessage} is reported.
 */
public final class CallPass implements Pass.Validation {

    @Override
    public void execute(Program program) throws ErrorMessage {
        final var analyzer = new CallAnalyzer(program);
        program.forEachInstruction(analyzer);
    }

    private static class CallAnalyzer extends DoNothingVoidRSSAVisitor {
        private final Program program;

        public CallAnalyzer(Program program) {
            this.program = program;
        }

        @Override
        protected void visitVoid(CallInstruction call) {
            if (!program.procedures().containsKey(call.procedure)) {
                throw new ErrorMessage.Undefined("procedure", call.procedure,
                        call.getSourcePosition().orElse(null));
            }

            final var calledProcedure = program.procedures().get(call.procedure);
            final BeginInstruction begin = calledProcedure.getBeginInstruction();
            final EndInstruction end = calledProcedure.getEndInstruction();

            final List<Atom> inputParameters = call.direction.choose(begin.getParameters(), end.getParameters());
            if (call.inputList.size() != inputParameters.size()) {
                throw new ErrorMessage.CallInputArgumentMismatch(call, inputParameters.size());
            }

            final List<Atom> outputParameters = call.direction.choose(end.getParameters(), begin.getParameters());
            if (call.outputList.size() != outputParameters.size()) {
                throw new ErrorMessage.CallOutputArgumentMismatch(call, outputParameters.size());
            }
        }
    }
}
