package rc3.lib.optimization;

import rc3.januscompiler.commandline.OptimizationsCommandSpec;
import rc3.lib.utils.CollectionUtils;
import rc3.lib.messages.ErrorMessage;

import java.io.PrintStream;
import java.util.*;

import static picocli.CommandLine.Model.CommandSpec;

/**
 * Instances of this class encapsulate the state of enabled, disabled and configured
 * {@link AvailableOptimization optimizations}.
 * <p>
 * This class provides methods to change the state of optimizations by
 * <ul>
 *  <li>{@link OptimizationState#setOptimizationEnabled(AvailableOptimization, boolean) enabling
 *  or disabling optimizations}</li>
 *  <li>{@link OptimizationState#addOptimizationParameter(AvailableOptimization, String)
 *  adding parameters to optimizations}</li>
 *  <li>{@link OptimizationState#setOptimizationLevel(int) changing the optimization level}</li>
 * </ul>
 * <p>
 * Similarly the state of optimizations can be fetched by
 * <ul>
 *  <li>{@link OptimizationState#isOptimizationEnabled(AvailableOptimization) checking
 *  whether an optimization is enabled}</li>
 *  <li>{@link OptimizationState#getOptimizationParameters(AvailableOptimization)
 *  getting the parameters of an optmization}</li>
 * </ul>
 * <p>
 * This class is also able to bind the state to a CLI application using the
 * {@link OptimizationState#bindToApplication(CommandSpec)} method.
 * Given a {@link CommandSpec} from <i>picocli</i>, it generates
 * options to control the behavior of optimizations via the command line.
 *
 * <pre>
 *  OptimizationState state = new OptimizationState();
 *
 *  public void setup() {
 *      CommandLine cli = new CommandLine(this);
 *      state.bindToApplication(cli.getCommandSpec());
 *  }
 * </pre>
 * <p>
 * Please note, that this class <b>does not</b> handle implicitly enabled
 * optimizations automatically. After the user input has been accepted and
 * before values from this class are used, a call to
 * {@link OptimizationState#enableImpliedOptimizations()} should be made,
 * to ensure that all implied optimizations are correctly enabled.
 */
public final class OptimizationState {
    private final Map<AvailableOptimization, Boolean> enabledOptimizations =
            new EnumMap<>(AvailableOptimization.class);
    private final Map<AvailableOptimization, List<String>> optimizationParameters =
            new EnumMap<>(AvailableOptimization.class);
    private int optimizationLevel = 0;

    private final Set<AvailableOptimization> enabledDebugModes =
            EnumSet.noneOf(AvailableOptimization.class);

    /**
     * Sets whether a given optimization is enabled.
     *
     * @param optimization The optimization to enable or disable.
     * @param isEnabled    <code>true</code> to enable an optimization,
     *                     <code>false</code> to disable.
     */
    public void setOptimizationEnabled(AvailableOptimization optimization, boolean isEnabled) {
        this.enabledOptimizations.put(optimization, isEnabled);
    }

    /**
     * Adds a parameter to an optimization.
     *
     * @param optimization The optimization to which the parameter
     *                     should be added.
     * @param parameter    The parameter to add.
     * @throws IllegalArgumentException If the optimization does not accept any parameters.
     */
    public void addOptimizationParameter(AvailableOptimization optimization, String parameter)
            throws IllegalArgumentException {
        if (!optimization.acceptsParameters()) {
            throw new IllegalArgumentException(
                    String.format("Optimization %s does not accept any parameters but %s was passed.",
                            optimization, parameter));
        }

        List<String> parameters = optimizationParameters.computeIfAbsent(optimization,
                ignored -> new ArrayList<>());
        parameters.add(parameter);
    }

    /**
     * Enables the debug mode for an optimization.
     *
     * @param optimization The optimization for which the debug mode should be enabled.
     */
    public void enableDebugForOptimization(AvailableOptimization optimization) {
        enabledDebugModes.add(optimization);
    }

    /**
     * Changes the optimization level which affects whether optimizations that are
     * not explicitly enabled or disabled by the user should count as enabled.
     *
     * @param level The level to set.
     */
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }


    private void enableImpliedOptimization(AvailableOptimization optimization, AvailableOptimization impliedBy) {
        if (!enabledOptimizations.containsKey(optimization)) {
            enabledOptimizations.put(optimization, true);
            for (AvailableOptimization impliedOptimization : optimization.getImpliedOptimizations()) {
                enableImpliedOptimization(impliedOptimization, impliedBy);
            }

        } else if (!enabledOptimizations.get(optimization)) {
            throw new ErrorMessage.RequiredDisabledOptimization(optimization, impliedBy);
        }
    }

    private Set<AvailableOptimization> allImpliedOptimizations(AvailableOptimization optimization) {
        return CollectionUtils.traverseGraph(() -> EnumSet.noneOf(AvailableOptimization.class),
                AvailableOptimization::getImpliedOptimizations, false, optimization);
    }

    /**
     * Enables all optimizations that should indirectly be enabled since another optimization
     * requires or implicitly enables them.
     *
     * @throws ErrorMessage.RequiredDisabledOptimization if a required or implicitly enabled
     *                                                   optimization was manually disabled.
     */
    public void enableImpliedOptimizations() {
        for (AvailableOptimization optimization : AvailableOptimization.values()) {
            if (isOptimizationEnabled(optimization)) {
                for (AvailableOptimization impliedOptimization : allImpliedOptimizations(optimization)) {
                    enableImpliedOptimization(impliedOptimization, optimization);
                }
            }
        }
    }

    /**
     * Checks whether an optimization was enabled by the user. If the user did not
     * explicitly specify the given optimization, the return value of this method depends
     * on whether the {@link AvailableOptimization} is enabled on the current optimization
     * level.
     */
    public boolean isOptimizationEnabled(AvailableOptimization optimization) {
        return enabledOptimizations.getOrDefault(optimization,
                optimization.isEnabledAtLevel(optimizationLevel) || enabledDebugModes.contains(optimization));
    }

    /**
     * Returns a {@link List} of parameters that were passed by the user while selecting an enabled optimization.
     * If no explicit parameters were passed, an empty list is returned.
     *
     * @throws IllegalArgumentException If the option does not accept any parameters.
     */
    public List<String> getOptimizationParameters(AvailableOptimization optimization) throws IllegalArgumentException {
        if (!optimization.acceptsParameters()) {
            throw new IllegalArgumentException("Parameters requested for optimization '" + optimization + "' that does not accept any parameters!");
        }
        return optimizationParameters.getOrDefault(optimization, Collections.emptyList());
    }

    /**
     * Returns <code>true</code> if the debug mode for this optimization was enabled.
     */
    public boolean isDebugModeEnabled(AvailableOptimization optimization) {
        return enabledDebugModes.contains(optimization);
    }


    ////////////////////////////////////////////////////////////////////////////////////
    // Application binding.
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Binds this state to the given {@link CommandSpec} and populates the
     * {@link CommandSpec} with options, allowing it to modify this state
     * via user inputs.
     *
     * @see OptimizationsCommandSpec
     */
    public void bindToApplication(CommandSpec application) {
        OptimizationsCommandSpec.populateCommandSpec(application, this,
                OptimizationsCommandSpec.DEFAULT_ORDER);
    }

    /**
     * Binds this state to the given {@link CommandSpec} and populates the
     * {@link CommandSpec} with options, allowing it to modify this state
     * via user inputs.
     * <p>
     * The additional order parameter controls where on the help page of the
     * {@link CommandSpec} the added commands are positioned.
     *
     * @see OptimizationsCommandSpec
     */
    public void bindToApplication(CommandSpec application, int order) {
        OptimizationsCommandSpec.populateCommandSpec(application, this, order);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Debug Interface
    ////////////////////////////////////////////////////////////////////////////////////

    private static final int LONGEST_OPTIMIZATION_NAME = Arrays.stream(AvailableOptimization.values())
            .map(AvailableOptimization::getCliName)
            .mapToInt(String::length)
            .max().orElse(0);

    private String showStateForOptimization(AvailableOptimization optimization) {
        String state = String.format("%s:%s %sabled",
                optimization.getCliName(),
                " ".repeat(LONGEST_OPTIMIZATION_NAME - optimization.getCliName().length()),
                isOptimizationEnabled(optimization) ? "en" : "dis");

        if (enabledDebugModes.contains(optimization)) {
            state += " (debug mode)";
        } else if (isOptimizationEnabled(optimization) && !enabledOptimizations.containsKey(optimization)) {
            state += " (by default)";
        }

        if (optimization.acceptsParameters()) {
            state += "; Parameters = " +
                    Arrays.toString(getOptimizationParameters(optimization).toArray());
        }

        return state;
    }

    public void dumpState(PrintStream dump) {
        dump.println("Optimization Level: " + optimizationLevel);
        for (AvailableOptimization optimization : AvailableOptimization.values()) {
            dump.println(showStateForOptimization(optimization));
        }
    }
}
