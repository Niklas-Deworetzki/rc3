package rc3.rssa.pass;

import rc3.lib.messages.ErrorMessage;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Program;
import rc3.rssa.instances.Variable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This pass is used to validate the creation and finalization of {@link Variable}.
 * <p>
 * An {@link ErrorMessage} is reported, if a one of the following cases occurs:
 * <ul>
 *  <li>A {@link Variable} is initialized, even though it is already live.</li>
 *  <li>A {@link Variable} is finalized, even though it is not live.</li>
 *  <li>A {@link Variable} is used, even though it is not live.</li>
 *  <li>A {@link Variable} is not finalized at the end of a block.</li>
 * </ul>
 */
public final class VariablePass implements Pass.Validation {

    @Override
    public void execute(Program program) throws ErrorMessage {
        for (ControlGraph procedure : program.procedures().values()) {
            final var validator = new BlockValidator(procedure.getProcedureName());
            for (BasicBlock block : procedure) {
                validator.validateVariables(block);
            }
        }
    }

    private static final class BlockValidator {
        private final String procedureName;
        private final Set<Variable> declaredInProcedure = new HashSet<>();
        private final Set<Variable> variablesInScope = new HashSet<>();

        public BlockValidator(String procedureName) {
            this.procedureName = procedureName;
        }

        public void validateVariables(BasicBlock block) {
            final Iterator<Instruction> instructions = block.iterator();

            final Instruction entry = instructions.next();
            creates(entry);
            uses(entry);

            instructions.forEachRemaining(instruction -> {
                creates(instruction);
                uses(instruction);
                destroys(instruction);
            });

            if (!variablesInScope.isEmpty()) {
                throw new ErrorMessage.UndestroyedVariables(block.exitPoint(), variablesInScope);
            }
        }

        private void creates(Instruction instruction) {
            for (Variable variable : instruction.variablesCreated()) {
                if (!variablesInScope.add(variable)) {
                    throw new ErrorMessage.VariableRedeclaration(variable, instruction);
                }
                if (!declaredInProcedure.add(variable)) {
                    throw new ErrorMessage.NotInSSA(procedureName, variable, instruction);
                }
            }
        }

        private void uses(Instruction instruction) {
            for (Variable variable : instruction.variablesUsed()) {
                if (!variablesInScope.contains(variable)) {
                    throw new ErrorMessage.UnknownVariable(variable, instruction);
                }
            }
        }

        private void destroys(Instruction instruction) {
            for (Variable variable : instruction.variablesDestroyed()) {
                if (!variablesInScope.remove(variable)) {
                    throw new ErrorMessage.UnknownVariable(variable, instruction);
                }
            }
        }
    }
}
