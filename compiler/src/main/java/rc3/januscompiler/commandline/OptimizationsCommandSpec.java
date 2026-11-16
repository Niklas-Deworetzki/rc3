package rc3.januscompiler.commandline;

import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.optimization.OptimizationState;

import java.util.stream.Collectors;

import static picocli.CommandLine.Model.*;

/**
 * This utility class is used to modify a {@link CommandSpec}, adding options
 * for available compiler optimizations.
 * <p>
 * Calling the only public method of this class, namely
 * {@link OptimizationsCommandSpec#populateCommandSpec(CommandSpec, OptimizationState, int)}
 * will add commands to an CLI application (represented by a {@link CommandSpec}), that allow
 * a user to enable, disable and configure {@link AvailableOptimization optimizations}.
 * The {@link OptionSpec OptionSpecs} introduced by this method, which represent the available
 * options for a user, are placed into two distinct {@link ArgGroupSpec groups}:
 * <ol>
 *  <li>An <i>Optimization Selection</i> group that provides three different kinds
 *  of {@link OptionSpec options}:</li>
 *  <ul>
 *      <li>Optimization level selector providing <code>-O</code> and <code>-On</code>
 *      to set the optimization level to 1 or the integer value of <var>n</var>.</li>
 *      <li>Optimization selector <code>-fOPT</code> that enabled the optimization
 *      named <var>OPT</var>.</li>
 *      <li>Optimization selector <code>-fno-OPT</code> that disables the
 *      optimization named <var>OPT</var>. These negative selectors will <b>not</b>
 *      be shown on the help page.</li>
 *  </ul>
 *  <li>An <i>Optimization Configuration</i> group that lists configuration options for
 *  optimizations. Note that {@link AvailableOptimization#acceptsParameters()} must
 *  return <code>true</code> for an optimization to appear in this group.</li>
 * </ol>
 * <p>
 * As an example, the help page will
 *
 * @apiNote It is not recommended, to use this class directly. Instead, allocate an
 * {@link OptimizationState} instance and use the public method
 * {@link OptimizationState#bindToApplication(CommandSpec)} to bind it to a CLI
 * and populate the CLI options of your application.
 */
public final class OptimizationsCommandSpec {
    private OptimizationsCommandSpec() {
    }

    /**
     * Do not use an explicit order when populating a {@link CommandSpec} with the different
     * {@link ArgGroupSpec} instances for optimizations.
     */
    public static final int DEFAULT_ORDER = -1;

    /**
     * Information for a selector whether to enable or disable an optimization.
     */
    private enum SelectionMode {
        ENABLE, DISABLE
    }

    /**
     * This method populates the given {@link CommandSpec} with a range of options to control, enable and
     * disable each {@link AvailableOptimization}. The given {@link CommandSpec} will be modified by
     * adding additional {@link OptionSpec OptionSpecs} wich are inserted into two newly created
     * {@link ArgGroupSpec ArgGroupSpecs}.
     * <p>
     * Additionally to the {@link CommandSpec} to be modified, this method also requires a
     * {@link OptimizationState} as one of its parameters. This {@link OptimizationState} is the underlying
     * state, that will be modified when a user accesses the created {@link OptionSpec commands}.
     * <p>
     * The final parameter of this method can be used to control, where the {@link ArgGroupSpec groups}
     * created by this method will be placed within the help page of the given {@link CommandSpec}.
     * If the static value {@link OptimizationsCommandSpec#DEFAULT_ORDER} is provided, no explicit order
     * is passed to the {@link ArgGroupSpec groups} and <i>picocli</i> will place them automatically.
     * For manual control, a order can be passed with the two generated groups being placed at
     * <code>order</code> and <code>order + 1</code>.
     *
     * @param commandSpec   The spec that should be populated with {@link OptionSpec OptionSpecs} for all
     *                      available optimizations.
     * @param state         The state that is modified by the generated {@link OptionSpec OptionSpecs}.
     * @param helpPageOrder An optional order which controls placement of the generated orders on the
     *                      help page. Use {@link OptimizationsCommandSpec#DEFAULT_ORDER} to let
     *                      <i>picocli</i> do this placement automatically.
     */
    public static void populateCommandSpec(CommandSpec commandSpec,
                                           OptimizationState state, int helpPageOrder) {
        ArgGroupSpec.Builder argGroupBuilder = createArgGroup(
                "%nOptimization options:%n", helpPageOrder, 0);

        argGroupBuilder.addArg(createOptimizationLevelOption(state));
        for (AvailableOptimization optimization : AvailableOptimization.values()) {
            argGroupBuilder = argGroupBuilder
                    .addArg(createSelectOption(optimization, SelectionMode.ENABLE, state))
                    .addArg(createSelectOption(optimization, SelectionMode.DISABLE, state));
        }

        OptionSpec debugOptimization = OptionSpec.builder("-d", "--debug-optimization")
                .paramLabel("optimization")
                .hideParamSyntax(true)
                .description("Outputs debug information for the selected optimization pass. " +
                        "Selected optimizations are enabled using this option.")
                .setter(new OptimizationDebugSelector(state))
                .type(AvailableOptimization.class)
                .build();
        argGroupBuilder.addArg(debugOptimization);

        commandSpec.addArgGroup(argGroupBuilder.build());

        // Add an additional group for parameters of configurations.
        ArgGroupSpec.Builder configGroupBuilder = createArgGroup(
                "%nConfiguration options for optimizations:%n", helpPageOrder, 1);

        for (AvailableOptimization optimization : AvailableOptimization.values()) {
            if (optimization.acceptsParameters()) {
                configGroupBuilder = configGroupBuilder.addArg(createArgumentOption(optimization, state));
            }
        }
        commandSpec.addArgGroup(configGroupBuilder.build());
    }

    /**
     * Creates an {@link ArgGroupSpec.Builder} with the given order and description.
     * <p>
     * The actual order used for the {@link ArgGroupSpec} is given by <code>order + offset</code>,
     * <b>if the order is not</b> {@link OptimizationsCommandSpec#DEFAULT_ORDER}, as in this case
     * no explicit order is set.
     */
    private static ArgGroupSpec.Builder createArgGroup(String description, int order, int orderOffset) {
        ArgGroupSpec.Builder builder = ArgGroupSpec.builder()
                .heading(description)
                .validate(false);

        if (order != DEFAULT_ORDER) { // Only set order if no default is used.
            builder.order(order + orderOffset);
        }

        return builder;
    }

    /**
     * Create an {@link OptionSpec} for the optimization level.
     * <p>
     * The created {@link OptionSpec} accepts <code>-O</code>, setting the optimization
     * level to 1, as well as <code>-On</code>, setting the level to the numerical
     * value of n.
     */
    private static OptionSpec createOptimizationLevelOption(OptimizationState state) {
        return OptionSpec.builder("-O")
                .description("Optimize. If a number is passed as an argument, the optimization level " +
                        "is set to the given number. Using this option without a number provided, is equal " +
                        "to selecting an optimization level of " + AvailableOptimization.DEFAULT_OPTIMIZATION_LEVEL +
                        ", enabling all safe optimizations that will not produce unexpected " +
                        "output. Note that compilation time will increase with a higher optimization level.")
                .paramLabel("n")
                .setter(new OptimizationLevelSetter(state))
                .arity("0..1")
                .type(String.class)
                .hideParamSyntax(true)
                .build();
    }

    /**
     * Create an {@link OptionSpec} to enable or disable an optimization, depending
     * on the <var>selectionMode</var>.
     *
     * <ul>
     *  <li>if <code>selectionMode == true</code> an {@link OptionSpec} for <code>-fOPT</code>
     *  is created to enable an optimization.</li>
     *  <li>if <code>selectionMode == false</code> an {@link OptionSpec} for <code>-fno-OPT</code>
     *  is created to disable an optimization.</li>
     * </ul>
     */
    private static OptionSpec createSelectOption(AvailableOptimization optimization, SelectionMode selectionMode,
                                                 OptimizationState state) {
        String prefix = selectionMode == SelectionMode.ENABLE ? "-f" : "-fno-";

        OptionSpec.Builder builder = OptionSpec.builder(prefix + optimization.getCliName())
                .setter(new OptimizationSelector(state, optimization, selectionMode));

        if (selectionMode == SelectionMode.ENABLE) {
            // Show positive variant with description.
            String description = optimization.getDescription();
            if (!optimization.getImpliedOptimizations().isEmpty()) {
                description += formatImpliedOptimizations(optimization);
            }

            builder = builder.description(description);
        } else {
            // Do not show negative variant in help.
            builder = builder.hidden(true);
        }
        return builder.build();
    }

    /**
     * Show a hint that one optimization implies others.
     */
    private static String formatImpliedOptimizations(AvailableOptimization optimization) {
        return " Implies: " + optimization.getImpliedOptimizations().stream()
                .map(AvailableOptimization::getCliName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Create an {@link OptionSpec} that allows parameters to be passed for an
     * optimization using <code>-COPT=par1,par2,par3</code>
     */
    private static OptionSpec createArgumentOption(AvailableOptimization optimization,
                                                   OptimizationState state) {

        return OptionSpec.builder("-C" + optimization.getCliName()) // -Cinline
                .description(optimization.getParameterDescription())
                .setter(new OptimizationParameterSelector(state, optimization))
                .paramLabel(optimization.getParameterLabel())
                .type(String.class)
                .arity("1..")
                .hideParamSyntax(true)
                .build();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Custom ISetter instances used to manipulate OptimizationState.
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * This class implements the {@link ISetter} interface, that is internally used by picocli to
     * handle selections via the command line.
     * <p>
     * Instances of this class are used to toggle an optimization on or off.
     */
    private record OptimizationSelector(OptimizationState state,
                                        AvailableOptimization selectedOptimization,
                                        SelectionMode selectionMode) implements ISetter {
        @Override
        public <T> T set(T t) {
            state.setOptimizationEnabled(selectedOptimization, selectionMode == SelectionMode.ENABLE);
            return null;
        }
    }

    /**
     * This class implements the {@link ISetter} interface, that is internally used by picocli to
     * handle selections via the command line.
     * <p>
     * Instances of this class are used to enable debug mode for optimizations.
     */
    private record OptimizationDebugSelector(OptimizationState state) implements ISetter {
        @Override
        public <T> T set(T t) {
            state.enableDebugForOptimization((AvailableOptimization) t);
            return null;
        }
    }

    /**
     * This class implements the {@link ISetter} interface, that is internally used by picocli to
     * handle selections via the command line.
     * <p>
     * Instances of this class are used to collect arguments for optimization configuration.
     * If configurations are passed to an optimization, it is implicitly enabled.
     */
    private record OptimizationParameterSelector(OptimizationState state,
                                                 AvailableOptimization selectedOptimization) implements ISetter {
        @Override
        public <T> T set(T t) {
            // Implicitly enable optimization if parameters are passed for it.
            state.setOptimizationEnabled(selectedOptimization, true);

            if (t instanceof String value && value.isEmpty()) {
                state.addOptimizationParameter(selectedOptimization, value);
            }
            return null;
        }
    }

    /**
     * This class implements the {@link ISetter} interface, that is internally used by picocli to
     * handle selections via the command line.
     * <p>
     * Instances of this class are used to set the optimization level.
     */
    private record OptimizationLevelSetter(OptimizationState state) implements ISetter {
        @Override
        public <T> T set(T t) {
            String levelDescription = (String) t;

            if (levelDescription.isEmpty()) {
                state.setOptimizationLevel(AvailableOptimization.DEFAULT_OPTIMIZATION_LEVEL);
            } else {
                try {
                    int level = Integer.parseInt(levelDescription);
                    state.setOptimizationLevel(level);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
    }
}
