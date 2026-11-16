package rc3.lib.utils;

import java.util.*;

public final class SetOperations {
    private SetOperations() {
        throw new UnsupportedOperationException("No instances of this class are allowed!");
    }

    /**
     * Returns <code>true</code> if the intersection of both {@link Set}s is non-empty.
     */
    public static <E> boolean intersects(Set<? extends E> a, Set<? extends E> b) {
        if (a.size() > b.size()) {
            return intersects(b, a);
        }

        for (E element : a) {
            if (b.contains(element))
                return true;
        }
        return false;
    }

    /**
     * Returns the union of both parameters, i.e. the {@link Set} holding all elements
     * that are element of either {@link Set}.
     * <p>
     * <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Venn0111.svg/384px-Venn0111.svg.png">
     */
    public static <E> Set<E> union(Set<? extends E> s1, Set<? extends E> s2) {
        final Set<E> result = new HashSet<>(s1);
        result.addAll(s2);
        return result;
    }

    /**
     * An extension of the {@link SetOperations#union(Set, Set) binary union operation}.
     * <p>
     * Returns the union of all parameters, i.e. the {@link Set} holding all elements that
     * are element of any parameter. If no parameters are given, an empty {@link Set} is
     * returned.
     */
    @SafeVarargs
    public static <E> Set<E> union(Set<? extends E>... sets) {
        final Set<E> result = new HashSet<>();
        for (Set<? extends E> set : sets) {
            result.addAll(set);
        }
        return result;
    }


    /**
     * Returns the intersection of both parameters, i.e. the {@link Set} holding all
     * elements that are elements of both {@link Set}s provided as parameters.
     * <p>
     * <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/99/Venn0001.svg/384px-Venn0001.svg.png">
     */
    public static <E> Set<E> intersection(Set<? extends E> s1, Set<? extends E> s2) {
        final Set<E> result = new HashSet<>(s1);
        result.retainAll(s2);
        return result;
    }

    /**
     * An extension of the {@link SetOperations#intersection(Set, Set) binary intersection
     * operation}.
     * <p>
     * Returns the intersection of all parameters, i.e. the {@link Set} holding all elements
     * that are element of all {@link Set}s provided as parameters. If no parameters are given,
     * the empty {@link Set} is returned.
     */
    @SafeVarargs
    public static <E> Set<E> intersection(Set<? extends E>... sets) {
        if (sets.length == 0) {
            return Set.of();
        }

        final Set<E> result = new HashSet<>(sets[0]);
        for (int i = 1; i < sets.length; i++) {
            result.retainAll(sets[i]);
        }
        return result;
    }

    /**
     * Returns the set-difference of both parameters, i.e. a {@link Set} holding
     * all elements that are element of the first parameter but are not element
     * of the second parameter. A copy of the first {@link Set} is created and
     * all elements of the second one are removed from the copy.
     * <p>
     * <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Venn0100.svg/384px-Venn0100.svg.png">
     *
     * @param original   A copy of this {@link Set} is created to subtract from.
     * @param subtracted The elements subtracted from the copy.
     */
    public static <E> Set<E> difference(Set<? extends E> original, Set<? extends E> subtracted) {
        final Set<E> result = new HashSet<>(original);
        result.removeAll(subtracted);
        return result;
    }

    /**
     * An extension of the {@link SetOperations#difference(Set, Set) binary set-difference operation}.
     * <p>
     * Returns a {@link Set} holding all elements of the first {@link Set} that are not element of
     * the remaining parameters. A copy of the first {@link Set} is created and subsequently,
     * elements of all remaining parameters are removed from this copy.
     *
     * @param original A copy of this {@link Set} is created to subtract from.
     * @param sets     The elements subtracted from the copy.
     */
    @SafeVarargs
    public static <E> Set<E> difference(Set<? extends E> original, Set<? extends E>... sets) {
        final Set<E> result = new HashSet<>(original);
        for (Set<? extends E> set : sets) {
            result.removeAll(set);
        }
        return result;
    }


    /**
     * Returns the symmetric difference of both parameters, i.e. the {@link Set}
     * holding all elements, that are in either of the parameters but not in both.
     * <p>
     * <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Venn0110.svg/384px-Venn0110.svg.png">
     */
    public static <E> Set<E> symmetricDifference(Set<? extends E> s1, Set<? extends E> s2) {
        final Set<E> result = union(s1, s2);
        result.removeAll(intersection(s1, s2));
        return result;
    }


    @SafeVarargs
    public static <E> List<E> concat(List<? extends E>... lists) {
        final List<E> result = new ArrayList<>();
        for (List<? extends E> list : lists) {
            result.addAll(list);
        }
        return result;
    }
}
