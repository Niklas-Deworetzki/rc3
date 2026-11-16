package rc3.rssa.pass;

import rc3.lib.messages.ErrorMessage;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.*;
import rc3.rssa.parse.ParsedProgram;

import java.util.*;

import static rc3.lib.utils.CollectionUtils.anyElement;

public final class ProceduresPass implements Pass.Transformation<ParsedProgram, Map<String, List<Instruction>>> {

    /**
     * Given a {@link ParsedProgram} this function will extract all defined procedures.
     * <p>
     * The contents of a procedure must be entirely contained between its begin and
     * end instruction. Additionally, a procedure is parsed as a set of basic blocks,
     * which must each begin with an entry point and end with an exit point.
     * Any instructions outside a procedure or block are reported as an error.
     * <p>
     * Furthermore, every procedure defined must be unique.
     *
     * @throws ErrorMessage If there are instructions outside a procedures or block,
     *                      a procedure or block is not closed correctly, a procedure has a conflict with
     *                      its begin and end labels, or if a procedure is defined multiple times.
     */
    @Override
    public Map<String, List<Instruction>> execute(ParsedProgram program) throws ErrorMessage {
        final Map<String, List<Instruction>> procedures = new HashMap<>();

        final Iterator<Instruction> instructions = program.instructions().iterator();
        while (instructions.hasNext()) {
            List<Instruction> procedure = consumeProcedure(instructions);

            ControlInstruction begin = (ControlInstruction) procedure.get(0);
            ControlInstruction end = (ControlInstruction) procedure.get(procedure.size() - 1);
            if (!begin.associatedLabels().equals(end.associatedLabels())) {
                // Labels of begin and end are not equal.
                throw new ErrorMessage.SubroutineMismatch(
                        anyElement(begin.associatedLabels()),
                        anyElement(end.associatedLabels()),
                        begin);
            }

            String label = anyElement(begin.associatedLabels());
            if (procedures.containsKey(label)) {
                // Subroutine was already defined.
                throw new ErrorMessage.RedeclarationAs("subroutine", label,
                        begin.getSourcePosition().orElse(null));
            }

            procedures.put(label, procedure);
        }

        validateEntryPoint(program.annotations(), procedures.keySet());
        return procedures;
    }

    /**
     * Checks if the entry point for this program is present.
     *
     * @throws ErrorMessage If multiple entry points are specified or if
     *                      the specified entry point is not present.
     */
    private static void validateEntryPoint(List<Annotation> annotations,
                                           Set<String> definedProcedures) throws ErrorMessage {
        boolean hasEntryAnnotation = false;

        final var entryPoints = Program.entryPoints(annotations);
        while (entryPoints.hasNext()) {
            if (hasEntryAnnotation) {
                // Multiple annotations for entry point.
                throw new ErrorMessage.MoreEntryPoints();
            }

            String entry = entryPoints.next();
            hasEntryAnnotation = true;
            if (!definedProcedures.contains(entry)) {
                // Specified entry point is not defined as procedure.
                throw new ErrorMessage.UnknownEntryPoint(entry);
            }
        }

        if (!hasEntryAnnotation && !definedProcedures.contains(Program.DEFAULT_MAIN_ENTRY)) {
            // Default entry point is not present.
            throw new ErrorMessage.UnknownEntryPoint(Program.DEFAULT_MAIN_ENTRY);
        }
    }

    /**
     * Consume a procedure from the {@link Iterator}.
     * <p>
     * A procedure starts with an {@link BeginInstruction} and ends with
     * an {@link EndInstruction}. Every {@link Instruction} between and
     * including those two instructions, is considered a procedure.
     *
     * @return A {@link List} of {@link Instruction Instructions} that represent
     * the consumed procedure.
     * @throws ErrorMessage If instructions are encountered outside a procedure,
     *                      outside a basic block, the procedure is not closed,
     *                      or a block is not closed.
     */
    private static List<Instruction> consumeProcedure(Iterator<Instruction> instructions) throws ErrorMessage {
        final List<Instruction> procedure = new ArrayList<>();

        consumeBlock(instructions, procedure);
        if (!(procedure.get(0) instanceof BeginInstruction)) {
            // Procedure does not start with Begin.
            throw new ErrorMessage.InstructionOutsideOfProcedure(procedure.get(0));
        }
        // Procedure is a single block.
        if (procedure.get(procedure.size() - 1) instanceof EndInstruction) {
            return procedure;
        }

        while (instructions.hasNext()) {
            consumeBlock(instructions, procedure);

            if (procedure.get(procedure.size() - 1) instanceof EndInstruction) {
                return procedure;
            }
        }

        // Procedure is not closed, but end of instructions is reached.
        throw new ErrorMessage.UnclosedProcedure((BeginInstruction) procedure.get(0));
    }

    /**
     * Consume a basic block, appending its instructions to the given buffer.
     * <p>
     * A procedure starts with an entry point and ends with
     * an exit point. Every {@link Instruction} between and
     * including those two instructions, is considered a block.
     *
     * @throws ErrorMessage If a block does not start with an entry
     *                      point or if it does not end with an exit point.
     */
    private static void consumeBlock(Iterator<Instruction> instructions,
                                     List<Instruction> blockBuffer) throws ErrorMessage {
        final Instruction first;
        Instruction current = first = instructions.next();
        blockBuffer.add(current);

        if (!(current instanceof ControlInstruction entry) || !entry.isEntryPoint) {
            // Block should start, but no entry point was found.
            throw new ErrorMessage.InstructionOutsideOfBlock(current);
        }

        do {
            current = instructions.next();
            blockBuffer.add(current);
        } while (!(current instanceof ControlInstruction) && instructions.hasNext());

        if (!(current instanceof ControlInstruction exit) || !exit.isExitPoint()) {
            // We encountered an entry point or the end of instructions.
            throw new ErrorMessage.UnclosedBlock((ControlInstruction) first);
        }
    }
}
