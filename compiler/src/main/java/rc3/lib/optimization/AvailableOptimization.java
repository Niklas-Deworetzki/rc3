package rc3.lib.optimization;

import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.commandline.OptimizationsCommandSpec;

import java.util.*;

/**
 * An enumeration listing the different optimizations implemented in at least one of the different compiler
 * {@link Backend}s.
 * <p>
 * The values listed in this enumeration are used to allow the user a selection of optimizations via the
 * command line. The <i>picocli</i> library is used to implement this behavior.
 * </p>
 * <p>
 * All enum values provide a name, that is used to select the corresponding optimization via the command line, as
 * well as a description, that is shown in usage texts. Additionally a boolean value can be supplied, indicating
 * whether or not an optimization should be enabled by default. If no boolean value is supplied, the optimization
 * is <b>disabled</b> by default.
 * </p>
 *
 * @see OptimizationsCommandSpec The interface between this optimization
 * enumeration and <i>picocli</i>.
 */
public enum AvailableOptimization {
    COMMON_SUBEXPRESSION_ELIMINATION("cse",
            "Reduces instruction count by locally eliminating duplicate computations.",
            1),

    GLOBAL_CONSTANT_PROPAGATION("gcp",
            "Propagates constants defined in the parameter list of basic blocks.",
            1),

    LOCAL_CONSTANT_PROPAGATION("lcp",
            "Propagates constants within a basic block.",
            1),

    INLINING("inline",
            "Inserts the body of a called procedure at call site to remove call overhead and allow for further optimizations. ",
            2),

    LIVE_VARIABLES("live-vars",
            "Analyses procedures to determine whether variables are live or dead.",
            Integer.MAX_VALUE),

    USED_VARIABLES("used-vars",
            "Analyses procedures to determine which variables are used.",
            Integer.MAX_VALUE),

    DEAD_CODE_ELIMINATION("dce",
            "Reduces instruction count by locally eliminating dead code and unused variables.",
            1),

    UNUSED_PROCEDURES_ELIMINATION("unused-procs",
            "Reduces instruction count by eliminating not called procedures.",
            1),

    ELIMINATE_UNREACHABLE("delete-unreachable",
            "Removes code paths that are unreachable both in forward and backward execution direction.",
            1),

    ELIMINATE_UNREACHABLE_UNSAFE("delete-unreachable-unsafe",
            "Removes code paths that are unreachable in forward or backward execution direction. " +
                    "This optimization might remove code that is only unreachable in a single execution " +
                    "direction and would cause runtime errors if executed in the other.",
            2),

    SIMPLIFY_CONTROL_FLOW("control",
            "Eliminates redundant jumps by merging basic blocks.",
            1),

    LOOP_OPTIMIZATION("loop-optimization",
            "Performs loop unrolling.",
            1);

    public static final int DEFAULT_OPTIMIZATION_LEVEL = 1;

    static {
        // The static initializer is executed AFTER the enum constants have been made available.
        // Use this block to configure implied optimizations and parameters.

        GLOBAL_CONSTANT_PROPAGATION.implies(LOCAL_CONSTANT_PROPAGATION);
        ELIMINATE_UNREACHABLE_UNSAFE.implies(ELIMINATE_UNREACHABLE);


        INLINING.withParameters("""
                        Numeric argument to set a maximal routine size to inline. Routines \
                        larger than the given size won't be inlined. Alternatively, names of \
                        routines to always inline can be provided. If prefixed with '!', \
                        routines with a given name will never be inlined.
                        """,
                "n", "name");

        LOOP_OPTIMIZATION.withParameters("""
                Numeric argument to set the maximum iteration count for loops to be unrolled. \
                """,
                "n");
    }

    /**
     * States that this optimization implies a set of other optimizations.
     */
    private void implies(AvailableOptimization... impliedOptimizations) {
        this.impliedOptimizations.addAll(Arrays.asList(impliedOptimizations));
    }

    /**
     * Adds a description for parameters to this optimization. This allows parameters
     * to be passed to this optimization.
     */
    private void withParameters(String description, String... parameterNames) {
        this.parameterDescription = description;
        this.parameterNames.addAll(Arrays.asList(parameterNames));
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Instance Variables, Getters and Setters.
    ////////////////////////////////////////////////////////////////////////////////////

    private final String cliName, description;
    private final int activationLevel;

    private final Set<AvailableOptimization> impliedOptimizations = new HashSet<>();

    private String parameterDescription = null;
    private final List<String> parameterNames = new ArrayList<>(0);

    AvailableOptimization(String cliName, String description, int activationLevel) {
        this.cliName = cliName;
        this.description = description;
        this.activationLevel = activationLevel;
    }

    /**
     * Returns a description of this optimization.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a command line friendly name for this optimization that is used to
     * select it via the command line.
     */
    public String getCliName() {
        return cliName;
    }

    /**
     * Returns whether this optimization is active at a given optimization level.
     *
     * @param optimizationLevel The current optimization level.
     * @return <code>true</code> if the optimization is active at the given level or
     * <code>false</code> otherwise.
     */
    public boolean isEnabledAtLevel(int optimizationLevel) {
        return optimizationLevel >= activationLevel;
    }

    /**
     * Returns a {@link Set} of other optimizations that should automatically be enabled if
     * this optimization is used, since it depends on them.
     */
    public Set<AvailableOptimization> getImpliedOptimizations() {
        return impliedOptimizations;
    }

    /**
     * Returns a description of the parameters this optimization accepts.
     *
     * @apiNote May be <code>null</code>.
     */
    public String getParameterDescription() {
        return parameterDescription;
    }

    /**
     * Returns whether this optimization accepts parameters.
     */
    public boolean acceptsParameters() {
        return parameterDescription != null;
    }

    /**
     * Returns a label that can be used to describe the function of the parameters.
     */
    public String getParameterLabel() {
        return String.join(",", parameterNames);
    }

    @Override
    public String toString() {
        return cliName;
    }
}

