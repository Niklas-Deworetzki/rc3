package rc3.januscompiler.backend;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * This abstract {@link NameResolver} provides methods to encode names for the use in a C program.
 * It respects all official reserved keywords of the C programming language as well as additional user
 * defined rules for names, that are not allowed as names for variables.
 */
public abstract class AbstractCNameResolver implements NameResolver {
    /**
     * All reserved keywords in C.
     */
    public static final Set<String> RESERVED_KEYWORDS = Set.of(
            "auto", "break", "case", "char", "const", "continue",
            "default", "do", "double", "else", "enum", "extern",
            "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed",
            "sizeof", "static", "struct", "switch", "typedef",
            "union", "unsigned", "void", "volatile", "while",
            "_Alignas", "_Alignof", "_Atomic", "_Bool", "_Complex",
            "_Generic", "_Imaginary", "_Noreturn", "_Static_assert", "_Thread_local",

            "main"
    );

    private final Predicate<String> isReservedLibraryName;

    public AbstractCNameResolver(Predicate<String> isReservedLibraryName) {
        this.isReservedLibraryName = isReservedLibraryName;
    }

    private static final Pattern LEGAL_CHARACTERS = Pattern.compile("[A-Za-z0-9]");

    private static String replaceIllegalChar(char character) {
        String charAsString = Character.toString(character);

        if (LEGAL_CHARACTERS.matcher(charAsString).matches()) {
            return charAsString;
        } else {
            return String.format("_%X", (int) character);
        }
    }

    public static String replaceIllegalChars(String identifier) {
        StringBuilder sb = new StringBuilder();
        for (char c : identifier.toCharArray()) {
            sb.append(replaceIllegalChar(c));
        }
        return sb.toString();
    }

    /**
     * Returns an available name that is valid in a C program.
     * <p>
     * If the proposed name is valid, it is returned as the result. Otherwise a numeric suffix is
     * added until a free name is found.
     * </p>
     *
     * @param namesInUse   A {@link Collection} of names that have already been given out within the active scope.
     *                     These names will not be returned as a result of this method.
     * @param proposedName A proposed name to be returned. If it is available, the proposed name is the result of
     *                     this method. Otherwise a numeric suffix is added, to create a free name.
     */
    public String findFreeName(Collection<String> namesInUse, String proposedName) {
        int cycle = 0;
        proposedName = replaceIllegalChars(proposedName);
        String name = proposedName;

        while (namesInUse.contains(name) || isReservedLibraryName.test(name) || RESERVED_KEYWORDS.contains(name)) {
            name = String.format("%s_%d", proposedName, cycle++);
        }

        return name;
    }
}
