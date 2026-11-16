package rc3.lib.utils;

/**
 * Utility class providing static methods to interact with {@link String Strings}.
 */
public final class StringUtils {
    private StringUtils() {
        throw new UnsupportedOperationException("No instances of this class are allowed!");
    }

    /**
     * Converts a number into a {@link String} representation using the given alphabet.
     * <p>
     * This is a generalization over the conversion from <code>int</code> to {@link String}
     * using a given base, since the characters used and their order (values) are entirely
     * determined by the given alphabet.
     * <p>
     * If a negative number is passed as an parameter, a negative sign '-' is added as a prefix.
     *
     * @param number   The number to convert.
     * @param alphabet The alphabet used for conversion.
     */
    public static String formatNumberWithAlphabet(int number, char[] alphabet) {
        return formatNumberWithAlphabet(number, alphabet, "-");
    }

    /**
     * Converts a number into a {@link String} representation using the given alphabet.
     * <p>
     * This is a generalization over the conversion from <code>int</code> to {@link String}
     * using a given base, since the characters used and their order (values) are entirely
     * determined by the given alphabet.
     * <p>
     * If a negative number is passed as an parameter, the given negative sign is added
     * as a prefix to the returned {@link String}.
     *
     * @param number       The number to convert.
     * @param negativeSign The prefix used when the input is negative.
     * @param alphabet     The alphabet used for conversion.
     */
    public static String formatNumberWithAlphabet(int number, char[] alphabet, String negativeSign) {
        StringBuilder result = new StringBuilder();

        if (number < 0) {
            result.append(negativeSign);
            number = -number;
        }

        do {
            result.append(alphabet[number % alphabet.length]);
            number /= alphabet.length;
        } while (number != 0);

        return result.toString();
    }

    /**
     * Escapes different whitespace characters, as well as single quotes, double quotes and
     * the backslash character.
     */
    public static String escape(char character) {
        return switch (character) {
            case '\b' -> "\\b";
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\f' -> "\\f";
            case '\r' -> "\\r";
            case '\"' -> "\\\"";
            case '\'' -> "\\'";
            case '\\' -> "\\\\";
            default -> Character.toString(character);
        };
    }

    /**
     * Escapes different whitespace characters, as well as single quotes, double quotes and
     * the backslash character.
     */
    public static String escape(String string) {
        StringBuilder sb = new StringBuilder();
        for (char c : string.toCharArray()) {
            sb.append(escape(c));
        }
        return sb.toString();
    }
}
