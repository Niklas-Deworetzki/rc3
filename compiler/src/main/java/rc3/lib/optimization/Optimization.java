package rc3.lib.optimization;

import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract superclass of {@link AvailableOptimization} implementations.
 * <p>
 * This class combines an {@link AvailableOptimization} with a concrete
 * {@link OptimizationState} in order to provide easy access for an
 * optimization's configuration.
 */
public abstract class Optimization {
    protected final AvailableOptimization optimization;
    protected final OptimizationState optimizationState;

    public Optimization(AvailableOptimization optimization, OptimizationState optimizationState) {
        this.optimization = optimization;
        this.optimizationState = optimizationState;
    }

    /**
     * Returns <code>true</code> if the optimization is enabled and should be executed.
     */
    public boolean isEnabled() {
        return optimizationState.isOptimizationEnabled(optimization);
    }

    /**
     * Returns <code>true</code> if the debug mode for this optimization is enabled.
     */
    public boolean isDebugEnabled() {
        return optimizationState.isDebugModeEnabled(optimization);
    }

    /**
     * Returns a {@link List} of {@link String} parameters provided as a configuration
     * for this optimization.
     *
     * @throws IllegalStateException If this optimization is not registered to accept
     *                               parameters.
     * @see AvailableOptimization#acceptsParameters()
     */
    public List<String> getOptimizationParameters() throws IllegalStateException {
        try {
            return optimizationState.getOptimizationParameters(optimization);
        } catch (IllegalArgumentException doesNotAcceptParameters) {
            throw new IllegalStateException("Optimization does not accept any parameters!",
                    doesNotAcceptParameters);
        }

    }

    /**
     * Produces debug output, if {@link Optimization#isDebugEnabled() debug mode is enabled}.
     */
    public void debug(String message, Object... format) {
        if (isDebugEnabled()) {
            System.out.printf(message + "%n", format);
        }
    }

    /**
     * Produces debug output, if {@link Optimization#isDebugEnabled() debug mode is enabled}.
     */
    public void debug(Supplier<String> messageSupplier) {
        if (isDebugEnabled()) {
            System.out.println(messageSupplier.get());
        }
    }
}
