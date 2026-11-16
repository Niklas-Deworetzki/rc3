package rc3.rssa.parse;

import rc3.rssa.annotations.Annotation;
import rc3.rssa.instances.Instruction;

import java.util.List;

/**
 * This class represents a RSSA program. It consists of a list of global annotations and a list of RSSA instructions.
 */
public record ParsedProgram(List<Instruction> instructions,
                            List<Annotation> annotations) {
}
