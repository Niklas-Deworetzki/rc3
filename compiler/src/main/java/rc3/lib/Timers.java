package rc3.lib;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class used to stop the runtime of actions. This class uses
 * {@link System#nanoTime()} to stop time.
 * <p>
 * Time can be stopped using the {@link Timers#start(String)} method when
 * an action begins and the {@link Timers#stop(String)} method when it
 * finishes.
 * <p>
 * Alternatively, the {@link Timers#time(String, Runnable)} or
 * {@link Timers#time(String, Supplier)} methods can be used to provide
 * the action that should be timed as a {@link Runnable} or {@link Supplier}.
 * <p>
 * To get the result of a previously stopped time, the {@link Timers#get(String)}
 * method is available.
 * <p>
 * If you wish to display the time stopped for some action, you can use the
 * {@link Timers#format(String, TimeUnit, int)} method, which allows to
 * specify a target {@link TimeUnit} as well as decimal precision.
 */
public final class Timers {
    private final Map<String, Long> startTimes = new HashMap<>();
    private final Map<String, Long> stoppedTimes = new HashMap<>();

    /**
     * Includes all running timers and stopped times from another
     * {@link Timers} instance in this object.
     */
    public void include(Timers other) {
        this.startTimes.putAll(other.startTimes);
        this.stoppedTimes.putAll(other.stoppedTimes);
    }

    /**
     * Returns <code>true</code> if a time has been stopped for the
     * given action.
     */
    public boolean contains(String action) {
        return stoppedTimes.containsKey(action);
    }


    /**
     * Start a timer for the given action.
     * <p>
     * If a timer for the given action was already started, it is reset.
     */
    public void start(String action) {
        startTimes.put(action, System.nanoTime());
    }

    /**
     * Stop the timer for the given action.
     *
     * @return The stopped time.
     * @throws IllegalStateException If no timer was started for the given action.
     */
    public long stop(String action) throws IllegalStateException {
        if (!startTimes.containsKey(action))
            throw new IllegalStateException("No timer has been started for " + action);

        final long timeDifference = System.nanoTime() - startTimes.remove(action);
        stoppedTimes.put(action, timeDifference);
        return timeDifference;
    }

    /**
     * Returns the stopped time for the given action.
     *
     * @return The stopped time.
     * @throws NoSuchElementException If no timer was stopped for the given action.
     */
    public long get(String action) throws NoSuchElementException {
        if (!contains(action))
            throw new NoSuchElementException("No time has been stopped for " + action);

        return stoppedTimes.get(action);
    }

    /**
     * Returns the stopped time for the given action.
     *
     * @param unit The {@link TimeUnit} to which the stopped time should be converted.
     * @return The stopped time.
     * @throws NoSuchElementException If no timer was stopped for the given action.
     */
    public long get(String action, TimeUnit unit) {
        return unit.convert(get(action), TimeUnit.NANOSECONDS);
    }

    /**
     * Times a given action.
     * <p>
     * Before the {@link Runnable} is executed, a timer is started.
     * After executing, it is stopped again.
     *
     * @param action   The action for which the time is stopped.
     * @param runnable The code to run for the action.
     */
    public void time(String action, Runnable runnable) {
        try {
            start(action);
            runnable.run();
        } finally {
            stop(action);
        }
    }

    /**
     * Times a given action.
     * <p>
     * Before the {@link Supplier} is executed, a timer is started.
     * After executing, it is stopped again.
     *
     * @param action   The action for which the time is stopped.
     * @param supplier The code to run for the action.
     * @return The result provided by the {@link Supplier}.
     */
    public <T> T time(String action, Supplier<T> supplier) {
        try {
            start(action);
            return supplier.get();
        } finally {
            stop(action);
        }
    }

    /**
     * Formats the time stopped for the given action. It is converted into
     * the given {@link TimeUnit} and returned as a {@link String} representing
     * an integer number.
     *
     * @param action The action for which the stopped time should be formatted.
     * @param unit   The {@link TimeUnit} to which the time should be converted.
     * @return A {@link String} representation of the stopped time.
     * @throws NoSuchElementException If no time was stopped for the given action.
     * @see Timers#format(String, TimeUnit, int) to control decimal places included in result.
     */
    public String format(String action, TimeUnit unit) throws NoSuchElementException {
        return format(action, unit, 0);
    }

    /**
     * Formats the time stopped for the given action. It is converted into
     * the given {@link TimeUnit} and returned as a {@link String}.
     * <p>
     * The returned {@link String} includes as many digits after the decimal
     * place, as it was provided by <var>precision</var>. If the parameter
     * set to 0, the decimal point is omitted.
     *
     * @param action    The action for which the stopped time should be formatted.
     * @param unit      The {@link TimeUnit} to which the time should be converted.
     * @param precision How many digits after the decimal place should be included.
     * @return A {@link String} representation of the stopped time.
     * @throws NoSuchElementException   If no time was stopped for the given action.
     * @throws IllegalArgumentException If the precision is set to a negative value.
     */
    public String format(String action, TimeUnit unit, int precision) throws NoSuchElementException, IllegalArgumentException {
        if (precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative!");
        }

        final long nanosecondDuration = get(action);
        if (precision == 0) {
            final long convertedTime = unit.convert(nanosecondDuration, TimeUnit.NANOSECONDS);
            return String.valueOf(convertedTime);
        } else {
            long exponent = 1;
            for (int i = 0; i < precision; i++) {
                exponent *= 10;
            }

            final long convertedScaledTime = unit.convert(nanosecondDuration * exponent, TimeUnit.NANOSECONDS);
            return String.format("%.2f", (double) convertedScaledTime / exponent);
        }
    }
}
