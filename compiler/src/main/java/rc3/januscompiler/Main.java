package rc3.januscompiler;

import picocli.CommandLine;
import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.commandline.BackendRenderer;
import rc3.lib.Application;
import rc3.lib.DumpOptions;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.optimization.OptimizationState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static picocli.CommandLine.*;

/**
 * The entry point of the Janus compiler framework.
 * <p>
 * This class creates a new {@link Compiler} instance with the parsed command line arguments and calls
 * the {@link Compiler#compile()} method.
 * </p>
 * <p>
 * The return code of this application depends on the result signaled by the {@link Compiler} or the users input.
 * <ul>
 *  <li>If {@link Compiler#compile()}</li> successfully returns, the exit code is {@link Main#EXIT_SUCCESS}.
 *  <li>If the users input cannot be parsed according to the specifications, the exit code is {@link Main#EXIT_USER_ERROR}.</li>
 *  <li>If an {@link ErrorMessage} is thrown during compilation, the exit code is {@link Main#EXIT_COMPILE_ERROR}.</li>
 *  <li>If an {@link IOException} is thrown during compilation, the exit code is {@link Main#EXIT_IO_ERROR}.</li>
 *  <li>If any other {@link Exception} is thrown, the exit code is {@link Main#EXIT_FATAL}.</li>
 * </ul>
 * </p>
 * <p>
 * An encountered {@link ErrorMessage} will be formatted and the error message is printed to the console.
 * If possible and not requested otherwise, the message will be printed in color.
 * </p>
 */
@Command(name = "rc3",

        synopsisHeading = "%n",
        abbreviateSynopsis = true,

        descriptionHeading = "%n",
        description = "The Reversible Computing Compiler Collection (RC3) is a research program to compile, optimize and " +
                "execute the reversible programming Janus.",

        parameterListHeading = "%nParameters:%n",

        optionListHeading = "%nOptions:%n",
        sortOptions = false,

        footerHeading = "%n",
        footer = "Copyright (C) 2021 Technische Hochschule Mittelhessen, University of Applied Sciences, Dept. MNI",

        usageHelpAutoWidth = true)
public class Main extends Application {

    // Constants used to order
    public static final int PICOCLI_ORDER_DEBUG = 100;
    public static final int PICOCLI_ORDER_OPTS = 90;

    @Parameters(index = "0", paramLabel = "INPUT",
            description = "The source file, that is used as an input for this program.")
    public File inputFile;

    @Parameters(index = "1", arity = "0..1", paramLabel = "OUTPUT",
            description = "An optional output file, where results are written to. " +
                    "If no file is specified, results are written to stdout instead.")
    public File outputFile = null; // Defaults to null (meaning stdout should be used).


    @Option(names = "--print-main",
            description = "Print the variables declared in the 'main' procedure after execution. This way the result " +
                    "of a program can be shown to the user, without the need of IO operations.%n" +
                    "The order of variables is implementation defined and depends on the selected backend.")
    public boolean shouldPrintMain;


    @Option(names = "--backend", paramLabel = "backend",
            description = "Available backends: ${COMPLETION-CANDIDATES}%n" + // ${COMPLETION-CANDIDATES} shows all enum values.
                    "Selects one of the possible compiler backends to be executed. The selected backend will " +
                    "run after all analysis passes are completed generating code to the given output file.")
    public Compiler.AvailableBackend backend;
    @Option(names = "--no-backend",
            description = "Don't execute backend. Only analysis passes will be executed.")
    public boolean noBackend;

    @Option(names = {"-b", "--backend-options"}, paramLabel = "option",
            description = "Additional options that are passed to the backend. Every backend may have a different set " +
                    "of accepted options, that can be used to tune the generated code.")
    public List<String> backendOptions = new ArrayList<>();


    @ArgGroup(validate = false,
            order = PICOCLI_ORDER_DEBUG,
            heading = "%nDebug options:%n")
    public DumpOptions dumpOptions = new DumpOptions();

    @Option(names = "-D",
            description = "Enables diagnostic output during the compilers execution.")
    public boolean diagnosticEnabled = false;

    // Mutated by picocli via OptimizationsCommandSpec
    public final OptimizationState optimizationState = new OptimizationState();

    public static void main(String[] args) {
        runApplication(args, new Main());
    }

    // Populated in initialize with the given arguments.
    private Backend backendInstance = null;

    @Override
    public void prepareCli(CommandLine cli) {
        optimizationState.bindToApplication(cli.getCommandSpec(), PICOCLI_ORDER_OPTS);
    }

    @Override
    public void initialize(ParseResult args) {
        this.backendInstance = Compiler.instantiateBackend(this.backend);

        // FIXME: --no-backend and --backend=<backend> are not mutually exclusive but should be.
        // Instantiate default backend, if no other backend was specified.
        if (backend == null && !noBackend) {
            this.backendInstance = Compiler.getDefaultBackend();
        }

        // Let backend parse options.
        if (this.backendInstance != null) {
            CommandLine.populateCommand(this.backendInstance, backendOptions.toArray(String[]::new));
        }
    }

    @Override
    public int displayHelp(CommandLine cli, Help.Ansi ansi) {
        if (this.backend != null && this.backendInstance != null) {
            // Backend was selected, show backend usage.
            new BackendRenderer(backendInstance).showUsage(System.out, ansi);
            return EXIT_SUCCESS;
        } else {
            // Without selected backend, show general usage page.
            return super.displayHelp(cli, ansi);
        }
    }

    @Override
    public int run() throws ErrorMessage, IOException, ParameterException {
        // Instantiate and invoke compiler.
        Compiler compiler = new Compiler(dumpOptions, shouldPrintMain, backendInstance, optimizationState, inputFile, outputFile);
        compiler.diagnosticEnabled = this.diagnosticEnabled;
        compiler.compile();
        return EXIT_SUCCESS;
    }
}

