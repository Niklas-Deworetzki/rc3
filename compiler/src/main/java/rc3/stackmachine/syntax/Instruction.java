package rc3.stackmachine.syntax;

import java.util.*;

/**
 * An {@link Instruction} is a reversible operation and accepts zero or a single {@link Operand}.
 */
public record Instruction(List<Operand.Symbol> labels, String mnemonic, Optional<Operand> operand) implements Line {
    private static final Map<String, String> REVERSIBLE_INSTRUCTION_PAIRS = new HashMap<>();

    /**
     * Create an {@link Instruction} without an {@link Operand}.
     */
    public Instruction(String mnemonic) {
        this(new ArrayList<>(0), mnemonic, Optional.empty());
    }

    /**
     * Create an {@link Instruction} with a given {@link Operand}.
     */
    public Instruction(String mnemonic, Optional<Operand> operand) {
        this(new ArrayList<>(0), mnemonic, operand);
    }

    @Override
    public List<Operand.Symbol> getLabels() {
        return labels;
    }

    @Override
    public String rawRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t").append(mnemonic);
        operand.ifPresent(op -> sb.append(' ').append(op));
        return sb.toString();
    }

    /**
     * Returns the inverse {@link Instruction}.
     */
    public Instruction reverse() {
        return new Instruction(labels, REVERSIBLE_INSTRUCTION_PAIRS.get(mnemonic), operand);
    }

    /**
     * Returns a {@link Set} of all known {@link Instruction} names.
     */
    public static Set<String> knownMnemonics() {
        return REVERSIBLE_INSTRUCTION_PAIRS.keySet();
    }

    /**
     * Checks whether a {@link String} represents a known {@link Instruction} name.
     */
    public static boolean isKnownMnemonic(String mnemonic) {
        return REVERSIBLE_INSTRUCTION_PAIRS.containsKey(mnemonic);
    }

    static {
        String[][] definedPairs = {
                {"halt", "halt"},
                {"nop", "nop"},
                {"pushc", "popc"},
                {"dup", "undup"},
                {"swap", "swap"},
                {"bury", "dig"},
                {"allocpar", "releasepar"},
                {"asf", "rsf"},
                {"pushl", "popl"},
                {"call", "call"},
                {"uncall", "uncall"},
                {"branch", "branch"},
                {"brt", "brt"},
                {"brf", "brf"},
                {"pushtrue", "poptrue"},
                {"pushfalse", "popfalse"},
                {"cmpusheq", "cmpopeq"},
                {"cmpushne", "cmpopne"},
                {"cmpushlt", "cmpoplt"},
                {"cmpushle", "cmpople"},
                {"inc", "dec"},
                {"neg", "neg"},
                {"add", "sub"},
                {"xor", "xor"},
                {"shl", "shr"},
                {"arpushadd", "arpopadd"},
                {"arpushsub", "arpopsub"},
                {"arpushmul", "arpopmul"},
                {"arpushdiv", "arpopdiv"},
                {"arpushmod", "arpopmod"},
                {"arpushand", "arpopand"},
                {"arpushor", "arpopor"},
                {"pushm", "popm"},
                {"load", "store"},
                {"memswap", "memswap"},
                {"xorhc", "xorhc"},
        };

        for (String[] pair : definedPairs) {
            String fw = pair[0];
            String bw = pair[1];

            REVERSIBLE_INSTRUCTION_PAIRS.put(fw, bw);
            if (!fw.equals(bw)) {
                REVERSIBLE_INSTRUCTION_PAIRS.put(bw, fw);
            }
        }
    }
}
