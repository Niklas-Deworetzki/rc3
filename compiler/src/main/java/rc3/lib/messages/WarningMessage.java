package rc3.lib.messages;

import org.fusesource.jansi.Ansi;
import rc3.lib.parsing.Position;
import rc3.rssa.annotations.views.ParamView;
import rc3.rssa.blocks.ControlGraph;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class WarningMessage implements Message {
    private final String message;
    private final Position position;

    public WarningMessage(Position position, String message, Object... args) {
        this.message = String.format(message, args);
        this.position = position;
    }

    @Override
    public final String header() {
        return "Warning";
    }

    @Override
    public final Ansi.Color color() {
        return Ansi.Color.YELLOW;
    }

    @Override
    public final Position getPosition() {
        return position;
    }

    @Override
    public final String getMessage() {
        return message;
    }

    @Override
    public final List<Map.Entry<String, Object>> additionalInfo() {
        return Collections.emptyList();
    }

    @Override
    public final String toString() {
        return Message.toString(this);
    }

    public final void report() {
        Ansi buffer = Ansi.ansi();
        toAnsi(buffer);
        System.out.println(buffer);
    }

    /**
     * A warning issued when a shorthand arithmetic assignment is used without parenthesis so that
     * the operators used indicate another precedence.
     * <pre>
     *  a := b + 2 << 3    // has a different precedence than the one
     *  a := b + (2 << 3)  // expanded to.
     * </pre>
     */
    public static class UnclearPrecedence extends WarningMessage {
        public UnclearPrecedence(Position position) {
            super(position, "Operator precedence might be unclear. The binary operand is always evaluated first, even if operators indicate otherwise.");
        }
    }

    /**
     * A warning issued when a {@link ParamView.Type} is not recognized.
     */
    public static class UnknownParamType extends WarningMessage {
        public UnknownParamType(ParamView view) {
            super(Position.NONE, "Annotation %s has unknown type %s. %s is used instead.",
                    view.getAnnotation(), view.getType(), ParamView.Type.INT);
        }
    }

    /**
     * A warning issued when the {@link ControlGraph} of a routine contains a conflict with
     * conditional entry and exit points that will lead to runtime errors.
     */
    public static class ControlFlowConflict extends WarningMessage {
        public ControlFlowConflict(ControlGraph graph) {
            super(graph.getBeginInstruction().getSourcePosition().orElse(Position.NONE),
                    "Control flow of routine `%s' is not reversible. This will likely cause runtime errors.",
                    graph.getProcedureName());
        }
    }

    public static class DisconnectedRoutine extends WarningMessage {
        public DisconnectedRoutine(ControlGraph graph) {
            super(graph.getBegin().entryPoint().getSourcePosition().orElse(Position.NONE),
                    "Routine `%s' will never finish execution gracefully. This will likely cause runtime errors.",
                    graph.getProcedureName());
        }
    }
}
