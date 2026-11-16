package rc3.lib.messages;

import org.fusesource.jansi.Ansi;
import rc3.lib.parsing.Position;

import java.util.List;
import java.util.Map;

/**
 * A mixin trait used for different {@link Message} types.
 * <p>
 * A message in general is some kind of information, represented by different components:
 * <ul>
 *  <li><b>A header</b> that communicates the message type and importance of the presented information.</li>
 *  <li><b>A text message</b> that shows the actual information.</li>
 *  <li><b>Additional information</b> that may be used to find where the information originates
 *  from or provide additional help.</li>
 *  <li><b>Markup information</b> used to highlight different parts of the message.</li>
 * </ul>
 */
public interface Message {
    /**
     * A special value to indicate, that no value is present for an {@link Message#additionalInfo()}
     * entry. This value must be used, since <code>null</code> might be a valid value.
     */
    Object NO_VALUE = new Object();

    /**
     * The message header identifying the message type. This is a string like
     * <code>"Error"</code> or <code>"Warning"</code>.
     */
    String header();

    /**
     * A {@link Ansi.Color Color} used to highlight different message attributes,
     * such as the message type or parts of the {@link Position}.
     */
    Ansi.Color color();


    /**
     * Returns the {@link Position} that is associated with the presented information.
     * If no such {@link Position} exists, this method should return {@link Position#NONE}.
     */
    Position getPosition();

    /**
     * Returns the information text for this message.
     */
    String getMessage();

    /**
     * Returns an ordered {@link List} of additional information. These {@link java.util.Map.Entry entries}
     * are pairs of a <i>header</i> and its <i>value</i>. If a value is {@link Message#NO_VALUE}, it is
     * not included in a {@link String} representation.
     */
    List<Map.Entry<String, Object>> additionalInfo();


    default void toAnsi(Ansi ansi) {
        Message.toAnsi(this, ansi);
    }

    /**
     * Returns the length of the longest key returned by {@link Message#additionalInfo()}.
     */
    default int longestKeyLength() {
        int longestKey = 0;
        for (Map.Entry<String, Object> entry : additionalInfo()) {
            longestKey = Math.max(longestKey, entry.getKey().length());
        }
        return longestKey;
    }

    static String toString(Message message) {
        final StringBuilder sb = new StringBuilder();

        sb.append(message.header()).append(": ").append(message.getMessage());

        final int longestKey = message.longestKeyLength();
        for (Map.Entry<String, Object> entry : message.additionalInfo()) {
            if (entry.getValue() != NO_VALUE) {
                sb.append("\n\t").append(entry.getKey()).append(':');
                sb.append(" ".repeat(longestKey + 1 - entry.getKey().length()));
                sb.append(entry.getValue());
            }
        }

        if (message.getPosition() != null && message.getPosition() != Position.NONE) {
            sb.append('\n').append(message.getPosition().toLongString());
        }

        return sb.toString();
    }

    static void toAnsi(Message message, Ansi ansi) {
        ansi.fg(message.color()).a(message.header()).a(": ").reset().a(message.getMessage());

        final int longestKey = message.longestKeyLength();
        for (Map.Entry<String, Object> entry : message.additionalInfo()) {
            if (entry.getValue() != NO_VALUE) {
                ansi.a("\n\t").a(entry.getKey()).a(':');
                ansi.a(" ".repeat(longestKey + 1 - entry.getKey().length()));
                ansi.a(entry.getValue());
            }
        }

        if (message.getPosition() != null && message.getPosition() != Position.NONE) {
            ansi.a('\n');
            message.getPosition().toAnsi(ansi, message.color());
        }
    }
}
