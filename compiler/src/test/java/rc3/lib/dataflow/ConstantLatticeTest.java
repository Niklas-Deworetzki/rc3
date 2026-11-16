package rc3.lib.dataflow;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ConstantLatticeTest {
    private final ConstantLattice<Integer> lattice = ConstantLattice.instance();

    @Test
    void constantsCanBeUnwrapped() {
        assertTrue(lattice.lift(5).get().isPresent(), "Constant should be present.");
        assertEquals(5, lattice.lift(5).unpack(), "Constant should not change.");
    }

    @Test
    void topCannotBeUnwrapped() {
        assertTrue(lattice.top().get().isEmpty(), "T should be empty.");
        assertThrowsExactly(NoSuchElementException.class, lattice.top()::unpack, "Unpacking T should raise an error.");
    }

    @Test
    void bottomCannotBeUnwrapped() {
        assertTrue(lattice.top().get().isEmpty(), "\uF05E should be empty.");
        assertThrowsExactly(NoSuchElementException.class, lattice.top()::unpack, "Unpacking \uF05E should raise an error.");
    }

    private void assertMeet(ConstantLattice.Value<Integer> expected,
                            ConstantLattice.Value<Integer> lhs,
                            ConstantLattice.Value<Integer> rhs) {
        assertEquals(expected, lattice.meet(lhs, rhs),
                String.format("%s &#8851; %s should equal %s.", lhs, rhs, expected));
    }

    @Test
    void testMeetProperties() {
        final var top = lattice.top();
        final var bot = lattice.bottom();
        final var x = lattice.lift(4);
        final var y = lattice.lift(5);

        assertMeet(top, top, top);
        assertMeet(x, top, x);
        assertMeet(bot, top, bot);

        assertMeet(x, x, top);
        assertMeet(x, x, x);
        assertMeet(bot, x, bot);

        assertMeet(y, y, top);
        assertMeet(bot, y, x);
        assertMeet(bot, y, bot);

        assertMeet(bot, bot, top);
        assertMeet(bot, bot, x);
        assertMeet(bot, bot, bot);
    }

    private void assertJoin(ConstantLattice.Value<Integer> expected,
                            ConstantLattice.Value<Integer> lhs,
                            ConstantLattice.Value<Integer> rhs) {
        assertEquals(expected, lattice.join(lhs, rhs),
                String.format("%s &#8852; %s should equal %s.", lhs, rhs, expected));
    }

    @Test
    void testJoinProperties() {
        final var top = lattice.top();
        final var bot = lattice.bottom();
        final var x = lattice.lift(4);
        final var y = lattice.lift(5);

        assertJoin(top, top, top);
        assertJoin(top, top, x);
        assertJoin(top, top, bot);

        assertJoin(top, x, top);
        assertJoin(x, x, x);
        assertJoin(x, x, bot);

        assertJoin(top, y, top);
        assertJoin(top, y, x);
        assertJoin(y, y, bot);

        assertJoin(top, bot, top);
        assertJoin(x, bot, x);
        assertJoin(bot, bot, bot);
    }
}