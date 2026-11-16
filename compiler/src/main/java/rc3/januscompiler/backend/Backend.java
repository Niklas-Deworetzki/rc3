package rc3.januscompiler.backend;


import rc3.januscompiler.Compiler;
import rc3.januscompiler.pass.Pass;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.table.Environment;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static picocli.CommandLine.Unmatched;


/**
 * An abstract class representing a compiler backend. The backend is executed after
 * all analysis {@link Pass Passes} have been performed.
 * <p>
 * The variable allocation pass is also part of the backend, since it requires knowledge about the architecture
 * for the backend is configured. For this reason the {@link Backend} accepts an optional {@link VariableAllocator}
 * as parameter, that is executed before the actual {@link Backend#execute(Compiler, Environment, Program, PrintStream)}
 * method is called.
 * </p>
 * <p>
 * A list of available {@link Backend Backends} is curated in the {@link Compiler.AvailableBackend} enum.
 * Adding an entry to this enumeration allows its selection via the command line if the according changes to
 * {@link Compiler#instantiateBackend(Compiler.AvailableBackend)} have been made.
 * </p>
 * <p>
 * The selection of a {@link Backend} via the command line (or another piece of code, that can manually access these)
 * is handled via the <i>picocli</i> library. To make backend options populate-able via the command line, they
 * have to be annotated with the {@link picocli.CommandLine.Option}.
 * </p>
 */
public abstract class Backend {
    protected final VariableAllocator allocator;

    @Unmatched
    public List<String> additionalOptions = new ArrayList<>();

    /**
     * Creates a new {@link Backend} instance using the given {@link VariableAllocator} to allocate variable sizes.
     * <p>
     * If the {@link VariableAllocator} is <code>null</code>, no allocation pass is performed.
     * </p>
     *
     * @param allocator The {@link VariableAllocator} used to allocate variable sizes.
     */
    public Backend(VariableAllocator allocator) {
        this.allocator = allocator;
    }

    public Backend() {
        this(null);
    }

    /**
     * Executes the {@link Backend} specific code.
     * <p>
     * This method is called after all analysis {@link Pass Passes} have been performed.
     * Thus it can access all information collected during those passes. If a {@link VariableAllocator} has been
     * passed to this class, the variable allocation pass will also have been performed before calling this method.
     * </p>
     *
     * @param compiler    The {@link Compiler} instance executing the {@link Backend}.
     * @param environment The global {@link Environment} containing the information of all global symbols.
     * @param program     The {@link Program} to be compiled by the {@link Backend}.
     * @param out         An {@link PrintStream} writing to the file specified when instantiating the {@link Compiler}.
     *                    It will be closed automatically afterwards.
     */
    protected abstract void execute(Compiler compiler, Environment environment, Program program, PrintStream out);

    /**
     * Executes the complete {@link Backend} by performing the variable allocation
     * if a {@link VariableAllocator} instance has been provided and afterwards calling
     * {@link Backend#execute(Compiler, Environment, Program, PrintStream)} to execute
     * the {@link Backend} specific code.
     * <p>
     * The handling of {@link Compiler.DumpOptions#dumpAllocation} is also implemented in this
     * method and will halt execution after the allocation if a dump was requested.
     * </p>
     *
     * @param compiler    The {@link Compiler} instance executing the {@link Backend}.
     * @param environment The global {@link Environment} containing the information of all global symbols.
     * @param program     The {@link Program} to be compiled by the {@link Backend}.
     * @param out         An {@link OutputStream} writing to the file specified when instantiating the {@link Compiler}.
     *                    It will be closed automatically afterwards.
     */
    public final void executeBackend(Compiler compiler, Environment environment, Program program, PrintStream out) {
        checkAdditionalOptions();

        boolean showAllocation = compiler.dumpOptions.dumpAllocation;

        if (allocator != null) {
            if (showAllocation) {
                System.out.printf("Running variable allocation for '%s'.\n", getDisplayName());

            }
            allocator.execute(compiler, environment, program);
        } else if (showAllocation) {
            System.out.printf("No allocator defined for '%s'.\n", getDisplayName());
        }

        if (showAllocation) {
            // Allocation dump requested, don't execute backend.
            return;
        }

        execute(compiler, environment, program, out);
    }

    private void checkAdditionalOptions() {
        if (!additionalOptions.isEmpty()) {
            System.err.println("WARNING: There are options that have not been recognized by the backend.");
            for (String additionalOption : additionalOptions) {
                System.err.println(" -> " + additionalOption);
            }
        }
    }

    /**
     * The {@link Backend Backends} name to be displayed in information messages.
     */
    public abstract String getDisplayName();

    /**
     * An optional description message, that can be provided to further describe this backend.
     */
    public abstract Optional<String> getDescription();

}
