package rc3.januscompiler.tac;

import rc3.januscompiler.Direction;
import rc3.januscompiler.table.ProcedureEntry;

/**
 * This class combines a {@link ProcedureEntry} with an {@link Direction} representing the
 * invocation of a procedure, as every procedure may be invoked in forward and backward direction.
 */
public record ProcedureInvocation(ProcedureEntry procedure,
                                  Direction direction) {
}
