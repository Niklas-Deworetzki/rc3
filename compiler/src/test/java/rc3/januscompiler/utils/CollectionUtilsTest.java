package rc3.januscompiler.utils;

import org.junit.jupiter.api.Test;
import rc3.lib.utils.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {

    @Test
    void testAnyElementIsReturned() {
        List<Object> aList = Arrays.asList(0, "Hello", 42, new Object());
        Object element = CollectionUtils.anyElement(aList);

        assertTrue(aList.contains(element), "List should contain returned element!");
    }

    @Test
    void testCollectReversedList() {
        List<Integer> numbers = IntStream.range(0, 10000).boxed().toList();
        List<Integer> reversedNumbers = numbers.stream().collect(CollectionUtils.collectReversedList());

        assertEquals(numbers.size(), reversedNumbers.size(),
                "Length of reversed list should match length of original list.");
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i),
                    reversedNumbers.get(reversedNumbers.size() - (i + 1)),
                    "Element at index " + i + " should be in reverse order.");
        }
    }

    @Test
    void testCollectReversedListParallel() {
        List<Integer> numbers = IntStream.range(0, 10000).boxed().toList();
        List<Integer> reversedNumbers = numbers.parallelStream().collect(CollectionUtils.collectReversedList());

        assertEquals(numbers.size(), reversedNumbers.size(),
                "Length of reversed list should match length of original list.");
        for (int i = 0; i < numbers.size(); i++) {
            assertEquals(numbers.get(i),
                    reversedNumbers.get(reversedNumbers.size() - (i + 1)),
                    "Element at index " + i + " should be in reverse order.");
        }
    }

    @Test
    void topologicalOrderTest() {
        Function<Integer, Collection<Integer>> smallerNumbers = n -> IntStream.range(0, n).boxed().collect(Collectors.toList());
        List<Integer> allNumbers = IntStream.rangeClosed(0, 100).boxed().collect(Collectors.toList());

        Collections.shuffle(allNumbers, new Random(97421397));

        List<Integer> topologicalOrder = CollectionUtils.topologicalOrder(allNumbers, smallerNumbers);

        int last = -1;
        for (Integer integer : topologicalOrder) {
            assertTrue(last < integer, "Topological order is not correct!");
            last = integer;
        }
    }

    @Test
    void topologicalOrderWithCyclesTest() {
        Function<Integer, Collection<Integer>> allDivisors = n ->
                IntStream.rangeClosed(1, n)
                        .filter(p -> n % p == 0)
                        .boxed()
                        .collect(Collectors.toList());


        Set<Integer> allNumbers = Stream.of(1, 3, 14, 36, 64, 88, 124)
                .flatMap(n -> allDivisors.apply(n).stream())
                .collect(Collectors.toSet());

        List<Integer> topologicalOrder = CollectionUtils.topologicalOrder(allNumbers, allDivisors);
        Set<Integer> encountered = new HashSet<>();

        for (Integer integer : topologicalOrder) {
            encountered.add(integer);
            assertTrue(encountered.containsAll(allDivisors.apply(integer)), "Topological order is not correct!");
        }
    }
}