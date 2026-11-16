package rc3.lib.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CollectionUtils {
    private CollectionUtils() {
        throw new UnsupportedOperationException("No instances of this class are allowed!");
    }

    /**
     * Returns a view on the given {@link List} where begin and end are truncated.
     *
     * @param list          The {@link List} of which a view should be created.
     * @param dropFromStart The amount of elements removed from the start of the {@link List}.
     * @param dropFromEnd   The amount of elements removed from the end of the {@link List}.
     * @throws IndexOutOfBoundsException if a negative amount of elements is requested to be
     *                                   truncated on either side of the {@link List}, or if
     *                                   the amount of elements to truncate is larger than the list.
     */
    public static <E> List<E> subList(List<E> list, int dropFromStart, int dropFromEnd) throws IndexOutOfBoundsException {
        return list.subList(dropFromStart, list.size() - dropFromEnd);
    }

    /**
     * Creates a {@link List} holding the given element <var>n</var> times.
     *
     * @param element The element with which the {@link List} is filled.
     * @param n       How many times the element should be included in the {@link List}.
     * @throws IllegalArgumentException if the amount of elements to generate is negative.
     * @implSpec The returned {@link List} if guaranteed to implement the {@link RandomAccess} interface.
     */
    public static <E> List<E> fillList(E element, int n) throws IllegalArgumentException {
        final List<E> list = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            list.add(element);
        }
        return list;
    }

    /**
     * Creates a {@link List} containing <var>n</var> elements. All elements are generated from the
     * given {@link Supplier}.
     *
     * @param elementSupplier A supplier used to generate <var>n</var> elements.
     * @param n               The amount of elements to be generated.
     * @throws IllegalArgumentException if the amount of elements to generate is negative.
     * @implSpec The returned {@link List} if guaranteed to implement the {@link RandomAccess} interface.
     */
    public static <E> List<E> fillList(Supplier<? extends E> elementSupplier, int n) throws IllegalArgumentException {
        final List<E> list = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            list.add(elementSupplier.get());
        }
        return list;
    }

    /**
     * Returns an arbitrary element of the given {@link Collection}.
     *
     * @param collection The collection of which an element is returned.
     * @return An arbitrary element of the given {@link Collection}.
     * @throws NoSuchElementException if the {@link Collection} does not contain any elements.
     */
    public static <T> T anyElement(Collection<T> collection) throws NoSuchElementException {
        return collection.iterator().next();
    }

    /**
     * Reverses any {@link Iterable} in linear additional time using linear additional space.
     * <p>
     * The reversion process is implemented by pushing every element of the {@link Iterable} onto
     * a {@link Deque} which is an {@link Iterable} on its own, traversing the elements in LIFO order.
     * </p>
     */
    public static <T> Iterable<T> reverse(Iterable<T> elements) {
        return reverse(elements, ArrayDeque::new);
    }

    /**
     * Reverses any {@link Iterable}.
     * <p>
     * All elements from the {@link Iterable} are pushed onto the {@link Deque} provided by the
     * passed {@link Supplier}. The {@link Deque} is then returned since it implements {@link Iterable}
     * and the {@link java.util.Iterator} returned by {@link Deque#iterator()} must iterate from top to
     * bottom, which is the reverse direction of the original {@link Iterable}.
     * </p>
     *
     * @param elements      The {@link Iterable} holding the elements to be reversed.
     * @param dequeSupplier A {@link Supplier} for a {@link Deque} that is used to reverse the elements.
     * @param <T>           The type of elements.
     * @return The {@link Deque} provided by the given {@link Supplier} holding the elements in reverse order.
     */
    public static <T> Iterable<T> reverse(Iterable<T> elements, Supplier<Deque<T>> dequeSupplier) {
        Deque<T> reverseElements = dequeSupplier.get();
        elements.forEach(reverseElements::push);
        return reverseElements;
    }

    /**
     * Traverses a Graph and returns a {@link Collection} holding all nodes gained from traversing said graph.
     * Traversal is performed in a breadth-first order.
     * <p>
     * The behavior of this method is largely defined by the provided data structures and mappings. The container
     * holding results is kept generic, so the user can decide, whether results should e.g. preserve order.
     * <p>
     * This method starts on some root nodes (which are optionally included) and traverses the given graph
     * starting from these roots. Every node connected to the roots is enqueued and while this queue is not
     * empty, the next node from it is removed. If this node is not contained in the result collection, all
     * connected nodes are enqueued. Otherwise, it is ignored.
     *
     * @param collectionProvider A {@link Supplier} providing a new instance of a {@link Collection} that is
     *                           used to store the traversed nodes.
     * @param edges              A function returning all neighboured nodes for a given node.
     * @param includeRoots       A <code>boolean</code> flag, whether the root nodes should be included
     *                           in the resulting collection.
     * @param roots              The root nodes from which traversal starts.
     * @param <N>                The type of nodes.
     * @param <R>                The type of the collection, holding traversed nodes.
     *                           (For example: <code>ArrayList::new</code>)
     * @return A new instance of the provided {@link Collection} type holding all traversed nodes.
     * @implNote This method eagerly traverses the given graph. It is therefore not suitable to traverse
     * infinite graphs.
     */
    @SafeVarargs
    public static <N, R extends Collection<N>> R traverseGraph(Supplier<R> collectionProvider,
                                                               Function<? super N, ? extends Iterable<N>> edges,
                                                               boolean includeRoots,
                                                               N... roots) {
        final R results = collectionProvider.get();
        final Queue<N> queuedNodes = new ArrayDeque<>();

        if (includeRoots) {
            Collections.addAll(queuedNodes, roots);
        } else {
            for (N root : roots) {
                edges.apply(root).forEach(queuedNodes::add);
            }
        }

        while (!queuedNodes.isEmpty()) {
            N node = queuedNodes.poll();
            if (!results.contains(node)) {
                results.add(node);
                edges.apply(node).forEach(queuedNodes::add);
            }
        }

        return results;
    }


    private static <N> boolean containsCycleHelper(Function<? super N, ? extends Iterable<N>> edges, N node,
                                                   Set<N> rootsOfAcyclicSubgraphsReference, Set<N> pathReference) {
        if (pathReference.contains(node)) return true; // Already visited, cycle detected.
        if (rootsOfAcyclicSubgraphsReference.contains(node))
            return false; // Previous iteration checked this node and its children.

        pathReference.add(node);
        for (N successor : edges.apply(node)) {
            if (containsCycleHelper(edges, successor, rootsOfAcyclicSubgraphsReference, pathReference)) return true;
        }
        pathReference.remove(node);

        rootsOfAcyclicSubgraphsReference.add(node); // Node does not contain any cycles. Store this info for later.
        return false;
    }

    /**
     * Checks whether a directed graph contains a cycle.
     * <p>
     * This method traverses the graph using a <i>depth-first</i> strategy, marking
     * its path on the way. If any path visits the same node twice, a cycle is must
     * be present and this method returns.
     * <p>
     * Traversal is performed for every node of the graph. This way, if the graph is
     * not connected, cycles can still be detected in unconnected subgraphs.
     * <p>
     * To increase performance, every acyclic subgraph is only checked once, marking
     * nodes that are roots of acyclic subgraphs, so that they can be skipped if they
     * are visited again.
     *
     * @param edges A {@link Function} representing the edges of a graph. Given a
     *              node this function should return all connected nodes.
     * @param nodes All nodes of the graph.
     * @param <N>   The type of nodes.
     * @return <code>true</code> if the given graph contains any cycles.
     * <code>false</code> otherwise.
     */
    public static <N> boolean containsCycle(Function<? super N, ? extends Iterable<N>> edges,
                                            Collection<N> nodes) {
        final Set<N> rootsOfAcyclicSubgraphs = new HashSet<>();
        final Set<N> path = new HashSet<>();
        for (N node : nodes) {
            if (containsCycleHelper(edges, node, rootsOfAcyclicSubgraphs, path)) {
                return true;
            }
            path.clear(); // Re-use path to avoid re-allocation of Set for every traversed node.
        }
        return false;
    }

    /**
     * Returns a {@link List} holding every node of the given graph. This {@link List} represents an order
     * in which the graph can be traversed, so that all successors of a node are visited before
     * said node is visited itself.
     * <p>
     * If the graph contains cycles or multiple such orderings exist, no guarantee is made, as to which order
     * is selected.
     *
     * @param nodes A {@link Collection} holding all nodes of the graph.
     * @param edges A mapping that returns all successors for a given node.
     */
    public static <N> List<N> topologicalOrder(Collection<N> nodes,
                                               Function<N, ? extends Iterable<N>> edges) {
        int nodeCount = nodes.size();
        List<N> order = new ArrayList<>(nodeCount);
        Set<N> visited = new HashSet<>(nodeCount);

        for (N node : nodes) {
            if (!visited.contains(node)) {
                topologicalOrderHelper(edges, visited, order, node);
            }
        }
        return order;
    }

    private static <N> void topologicalOrderHelper(Function<N, ? extends Iterable<N>> edges,
                                                   Set<N> visitedNodesReference,
                                                   List<N> orderReference,
                                                   N node) {
        visitedNodesReference.add(node);

        for (N child : edges.apply(node)) {
            if (!visitedNodesReference.contains(child)) {
                topologicalOrderHelper(edges, visitedNodesReference, orderReference, child);
            }
        }

        orderReference.add(node);
    }

    /**
     * Eagerly applies the action of the given {@link BiConsumer} on every pair of the given elements.
     */
    public static <L, R> void combineWith(Iterable<L> left, Iterable<R> right, BiConsumer<L, R> action) {
        final Iterator<L> lefts = left.iterator();
        final Iterator<R> rights = right.iterator();

        while (lefts.hasNext() && rights.hasNext()) {
            action.accept(lefts.next(), rights.next());
        }
    }


    /**
     * Lazily zips together two collections using the given combinator.
     * <p>
     * This method creates a {@link Stream} that combines two elements from the given collections each by
     * applying the given combinator to them.
     */
    public static <L, R, T> Stream<T> zipWith(Collection<? extends L> left,
                                              Collection<? extends R> right,
                                              BiFunction<? super L, ? super R, ? extends T> combinator) {
        Spliterator<T> zipped = Spliterators.spliterator(
                new Zipper<>(left, right, combinator),
                Math.min(left.size(), right.size()),
                Spliterator.SIZED);
        return StreamSupport.stream(zipped, false);
    }

    private static final class Zipper<L, R, T> implements Iterator<T> {
        private final Iterator<? extends L> ls;
        private final Iterator<? extends R> rs;
        private final BiFunction<? super L, ? super R, ? extends T> combinator;

        public Zipper(Iterable<? extends L> ls, Iterable<? extends R> rs,
                      BiFunction<? super L, ? super R, ? extends T> combinator) {
            this.ls = ls.iterator();
            this.rs = rs.iterator();
            this.combinator = combinator;
        }

        @Override
        public boolean hasNext() {
            return ls.hasNext() && rs.hasNext();
        }

        @Override
        public T next() {
            return combinator.apply(ls.next(), rs.next());
        }
    }


    /**
     * Creates a new {@link Collector} instance, that collects the elements of a {@link Stream} into a list in
     * reverse order. This is similar to {@link Collectors#toList()} with the distinction that the returned list
     * holds the elements in reversed order.
     * <p>
     * To provide a performant implementation, the underlying {@link List} is a {@link LinkedList} since it
     * supports constant-time prepend operations.
     *
     * @param <T> The type of elements collected to a list.
     */
    public static <T> Collector<? super T, LinkedList<T>, List<T>> collectReversedList() {
        return new Collector<>() {
            @Override
            public Supplier<LinkedList<T>> supplier() {
                return LinkedList::new;
            }

            @Override
            public BiConsumer<LinkedList<T>, T> accumulator() {
                return LinkedList::addFirst;
            }

            @Override
            public BinaryOperator<LinkedList<T>> combiner() {
                return (left, right) -> {
                    right.addAll(left);
                    return right;
                };
            }

            @Override
            public Function<LinkedList<T>, List<T>> finisher() {
                return list -> list;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.IDENTITY_FINISH);
            }
        };
    }

    /**
     * Allows to iterate the given {@link Iterable} without encountering any element twice.
     * <p>
     * The {@link Iterable} returned by this method is backed by {@link Iterator Iterators} produced by
     * the given {@link Iterable}. Consequently, the order of elements is preserved, while duplicates
     * are filtered and removed from the produced {@link Iterator Iterators} output.
     */
    public static <T> Iterable<T> makeUnique(Iterable<T> source) {
        return () -> new UniqueIterator<>(source.iterator());
    }

    /**
     * Allows to iterate the given {@link Iterator} without encountering any element twice.
     * <p>
     * The {@link Iterator} returned by this method is backed by the given {@link Iterator}.
     * Consequently, the order of elements is preserved, while duplicates are filtered and
     * removed from the returned {@link Iterator Iterators} output.
     */
    public static <T> Iterator<T> makeUnique(Iterator<T> source) {
        return new UniqueIterator<>(source);
    }

    private static final class UniqueIterator<T> implements Iterator<T> {
        private final Iterator<T> source;
        private final Set<T> alreadyVisited = new HashSet<>();

        private T next;
        private boolean hasNext;

        public UniqueIterator(Iterator<T> source) {
            this.source = source;
            findNext();
        }

        private void findNext() {
            while (source.hasNext()) {
                T next = source.next();
                if (alreadyVisited.add(next)) {
                    this.next = next;
                    this.hasNext = true;
                    return;
                }
            }
            this.hasNext = false;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext) return source.next();

            T result = next;
            findNext();
            return result;
        }
    }

    /**
     * Counts the occurrences of elements in the given {@link Iterable} and returns a {@link Map} where the
     * keys represent the original {@link Iterable Iterables} elements. The value bound to every key is the
     * amount of times, said key occurred in the {@link Iterable}.
     */
    public static <E> Map<E, Integer> countOccurrences(Iterable<E> es) {
        Map<E, Integer> occurrences = new HashMap<>();
        for (E e : es) {
            occurrences.put(e, 1 + occurrences.getOrDefault(e, 0));
        }
        return occurrences;
    }

    /**
     * Creates a primitive {@link Stream} from an {@link Iterable}.
     */
    public static <E> Stream<E> stream(Iterable<E> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
