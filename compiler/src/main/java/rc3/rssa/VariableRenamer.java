package rc3.rssa;

import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Constant;
import rc3.rssa.instances.Instruction;
import rc3.rssa.instances.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static rc3.lib.utils.CollectionUtils.zipWith;

/**
 * A support class to aid renaming {@link Variable} instances in {@link Instruction}s.
 * <p>
 * Instead of directly using {@link Instruction#replaceVariable(Variable, Atom)}, this
 * class accepts a collection of replacements and verifies them, before replacing
 * {@link Variable}s.
 * <p>
 * Replacements can either be specified using two parameter lists, using the
 * {@link VariableRenamer#fromParameterLists(List, List)} method. Or they can
 * be constructed from a {@link Map} using the {@link VariableRenamer#fromMap(Map)}
 * method.
 * <p>
 * If a replacement aims to replace a {@link Constant} with a different {@link Constant},
 * a conflict is marked and this class will not perform any replacements at all.
 * During construction of an internal lookup {@link Map}, these <i>conflicts</i> are
 * found and an according internal flag is set. The complete lookup {@link Map}
 * is still constructed, however, and can be fetched using {@link VariableRenamer#getReplacements()}.
 */
public final class VariableRenamer implements Consumer<Instruction> {

    private final Map<Variable, Atom> replacements;
    private final boolean hasConflicts;

    private VariableRenamer(Map<? extends Atom, ? extends Atom> unfilteredReplacements) {
        this.replacements = new HashMap<>(unfilteredReplacements.size());
        this.hasConflicts = constructReplacementMap(unfilteredReplacements, this.replacements);
    }

    private static boolean constructReplacementMap(Map<? extends Atom, ? extends Atom> unfilteredReplacements,
                                                   Map<Variable, Atom> validReplacements) {
        boolean foundConflicts = false;
        for (final var entry : unfilteredReplacements.entrySet()) {
            if (entry.getKey() instanceof Variable variable) {
                validReplacements.put(variable, entry.getValue());
            } else if (!Objects.equals(entry.getKey(), entry.getValue())) {
                foundConflicts = true;
            }
        }
        return foundConflicts;
    }

    /**
     * Returns a {@link Map} of replacements used by this instance.
     */
    public Map<Variable, Atom> getReplacements() {
        return replacements;
    }

    /**
     * Returns <code>true</code>, if conflicts were found in the replacement {@link Map}.
     */
    public boolean hasConflicts() {
        return hasConflicts;
    }

    @Override
    public void accept(Instruction instruction) {
        replacements.forEach(instruction::replaceVariable);
    }

    /**
     * Constructs a {@link VariableRenamer}, renaming every parameter in the first parameter list to
     * its equivalent parameter in the second parameter list.
     */
    public static VariableRenamer fromParameterLists(List<? extends Atom> from, List<? extends Atom> to) {
        final var replacements = zipWith(from, to, Map::entry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new VariableRenamer(replacements);
    }

    /**
     * Constructs a {@link VariableRenamer}, renaming all {@link Variable} instances using the
     * mappings provided by the given {@link Map}.
     */
    public static VariableRenamer fromMap(Map<? extends Atom, ? extends Atom> replacements) {
        return new VariableRenamer(replacements);
    }
}
