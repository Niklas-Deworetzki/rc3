package rc3.lib;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;
import rc3.januscompiler.SourceConfig;
import rc3.lib.messages.ErrorMessage;

import java.io.IOException;
import java.io.PrintStream;

import static picocli.CommandLine.Help;
import static picocli.CommandLine.ParseResult;

/**
 * This abstract {@link Application} class allows command-line facing applications to provide
 * the common functionality of parsing arguments, catching common errors and providing application
 * exit codes to the user.
 * <p>
 * An application implementing this class should provide a <tag>static void main(String[])</tag>
 * method as an entry point for a program. This method could then call the static
 * {@link Application#runApplication(String[], Application)} method, which executes the program,
 * handling the common library code.
 * <pre>
 *  public static void main(String[] args) {
 *      runApplication(args, new MyCommandLineApplication());
 *  }
 * </pre>
 *
 * <p>
 * This abstract {@link Application} class provides multiple methods to hook into the application
 * lifecycle.
 * <ol>
 *  <li>
 *      Before any command line arguments are parsed, the method {@link Application#prepareCli(CommandLine)}
 *      allows to modify {@link CommandLine} specification of the argument parser.
 *      This will change the behavior of <i>picocli</i>, which is used to parse user arguments.
 *      Alternatively instance variables of a subclass can be supplied with the {@link picocli.CommandLine.Option}
 *      annotation, which will automatically instruct <i>picocli</i> to use this instance variable
 *      as one of the possible arguments.
 *  </li>
 *  <li>
 *      After the command line arguments are parsed using the {@link CommandLine} specification, the method
 *      {@link Application#initialize(ParseResult)} is called with the {@link ParseResult} obtained from
 *      parsing the command line arguments.
 *      This method allows initialization of components dependent on user provided input.
 *  </li>
 *  <li>
 *      After command line parsing and initialization, the application is run.
 *      Common command line options are checked, controlling the color output and calling
 *      into help or version output methods, if they are requested by the user.
 *      The application is run afterwards by calling the {@link Application#runApplication(String[], Application)}
 *      method, which can supply an application exit code by returning it as its integer return value.
 *  </li>
 *  <li>
 *      The final part of the application lifecycle is the cleanup phase, which is initialized
 *      by calling {@link Application#cleanup()}. This method is called immediately before returning
 *      the application-provided exit code.
 *  </li>
 * </ol>
 *
 * <p>
 * During its lifecycle, an {@link Application} provided the following functionality and default
 * implementations.
 * <ul>
 *  <li>
 *      <b>Color Management</b>
 * <p>
 *      If a user provides <tt>--color</tt> or <tt>--no-color</tt> as an explicit argument,
 *      the {@link Ansi} mode is changed, to either allow or disallow colored output. Without
 *      one of these flags provided by the user, a default mode is used.
 * <p>
 *      The color mode affects the output produced by the application, the help page as well as
 *      error handling messages. The help page is formatted using {@link Help.Ansi} from the
 *      <i>picocli</i> library, while {@link Ansi} is registered, managed and cleaned up as well
 *      for application and error output.
 *  </li>
 *  <li>
 *      <b>Help Page</b>
 * <p>
 *      If a user provides <tt>--help</tt> as an argument, a help page will be displayed
 *      <b>instead of</b> running the application. This is done by calling
 *      {@link Application#displayHelp(CommandLine, Help.Ansi)} which can provide an exit
 *      code for the application.
 * <p>
 *      The default implementation of {@link Application#displayHelp(CommandLine, Help.Ansi)}
 *      will show the {@link CommandLine#usage(PrintStream) command line usage}.
 *  </li>
 *  <li>
 *      <b>Version Information</b>
 * <p>
 *      If a user provides <tt>--version</tt> as an argument, the current version information
 *      will be displayed <b>instead of</b> running the application. This is done by
 *      {@link SourceConfig#initialize() initializing the SourceConfig} and fetching
 *      its {@link SourceConfig#getVersionString()}.
 *  </li>
 *  <li>
 *      <b>Error Reporting</b>
 * <p>
 *      Errors within the application are automatically caught and reported. Depending on
 *      the lifecycle phase, an application method may throw any {@link IOException},
 *      {@link CommandLine.ParameterException}, {@link ErrorMessage} or any of their
 *      subclasses.
 * <p>
 *      {@link ErrorMessage} instances will be reported using {@link ErrorMessage#toAnsi(Ansi)}
 *      which allows them to print colored output, as well as a detailed problem description
 *      and potentially location data.
 *      The exit code in this case is {@link Application#EXIT_COMPILE_ERROR}.
 * <p>
 *      {@link CommandLine.ParameterException} instances will be caught and their error message
 *      will be shown if some user arguments were provided. If <b>no user arguments</b> are
 *      present, the help page will be displayed instead by calling
 *      {@link Application#displayHelp(CommandLine, Help.Ansi)}.
 *      The exit code in this case is {@link Application#EXIT_USER_ERROR}.
 * <p>
 *      {@link IOException} instances will be reported by printing their associated message.
 *      The exit code in this case is {@link Application#EXIT_IO_ERROR}.
 * <p>
 *      Other {@link Exception} instances are reported by {@link Exception#printStackTrace() printing
 *      their stack trace}. In these cases the application will exit with an exit code
 *      of {@link Application#EXIT_FATAL}.
 *  </li>
 * </ul>
 */
public abstract class Application {
    // Application exit codes.
    /**
     * The Application exited successful.
     */
    public static final int EXIT_SUCCESS = 0;
    /**
     * A generic error occurred during the runtime of the application.
     * It could not complete its main purpose successfully.
     */
    public static final int EXIT_COMPILE_ERROR = 1;
    /**
     * The provided user input or configuration parameters are
     * invalid or could not be parsed. The application could not
     * complete its main purpose successfully as a result of that.
     */
    public static final int EXIT_USER_ERROR = 2;
    /**
     * A fatal, unexpected exception occurred during the applications
     * runtime. This is often the case with an uncaught {@link RuntimeException}.
     */
    public static final int EXIT_FATAL = 70;
    /**
     * An {@link IOException} occurred during runtime, which hinders the
     * application to perform its main purpose.
     */
    public static final int EXIT_IO_ERROR = 74;

    @CommandLine.ArgGroup(validate = false, heading = "%nApplication options:%n")
    public ApplicationOptions applicationOptions = new ApplicationOptions();

    /**
     * Allows the {@link Application} to modify the {@link CommandLine} specification before
     * user input is parsed.
     *
     * @throws IOException                    to signal that important IO resources
     *                                        are unavailable.
     * @throws CommandLine.ParameterException to signal that the cli configuration
     *                                        is invalid.
     */
    public void prepareCli(CommandLine cli) throws IOException, CommandLine.ParameterException {
        // Intentionally left empty. Subclasses can specify behavior.
    }

    /**
     * Initializes the {@link Application} with the parameters received from parsing
     * the user input and allows for additional initialization over the default
     * provided by <i>picocli</i>.
     *
     * @throws IOException                    to signal that important IO resources
     *                                        are unavailable.
     * @throws CommandLine.ParameterException to signal that the cli configuration
     *                                        is invalid.
     */
    public void initialize(ParseResult args) throws IOException, CommandLine.ParameterException {
        // Intentionally left empty. Subclasses can specify behavior.
    }

    /**
     * Runs the {@link Application}, letting it perform its main purpose.
     *
     * @return An exit code for the application.
     * @throws ErrorMessage to signal an abnormal or invalid state detected during runtime.
     * @throws IOException  to signal that important IO resources are unavailable.
     */
    public abstract int run() throws ErrorMessage, IOException;

    /**
     * Lets the {@link Application} clean up its resources.
     *
     * @throws IOException to signal that some important IO error occurred during
     *                     cleanup that should be reported to the user.
     */
    public void cleanup() throws IOException {
        // Intentionally left empty. Subclasses can specify behavior.
    }

    /**
     * Shows a help page.
     *
     * @return An exit code that should be returned for a user that requested
     * the help page.
     */
    public int displayHelp(CommandLine cli, Help.Ansi ansi) {
        cli.usage(System.out, ansi);
        return EXIT_SUCCESS;
    }

    /**
     * Run the {@link Application} handling all common library code.
     *
     * @param args The command line arguments provided for the {@link Application}.
     * @return An exit code as a result for this {@link Application}.
     */
    public final int execute(String[] args) {
        CommandLine cli = new CommandLine(this);
        Help.Ansi cliAnsi = CommandLine.Help.Ansi.AUTO;

        try {
            // Prepare picocli and parse arguments.
            prepareCli(cli);
            initialize(cli.parseArgs(args));

            // Apply color options.
            if (applicationOptions.coloredOutput != null) {
                Ansi.setEnabled(applicationOptions.coloredOutput);
                cliAnsi = (applicationOptions.coloredOutput) ? Help.Ansi.ON : Help.Ansi.OFF;
            }

            // Exit early if version info was requested.
            if (applicationOptions.requestedVersionInfo) {
                SourceConfig.initialize();
                System.out.println(SourceConfig.getVersionString());
                return EXIT_SUCCESS;
            }

            // Exit early if usage info was requested.
            if (applicationOptions.requestedHelp) {
                return displayHelp(cli, cliAnsi);
            }

            int exitCode = run();
            cleanup();
            return exitCode;

        } catch (ErrorMessage errorMessage) {
            // CompileErrors have a meaningful message, which should be displayed to the user.
            reportCompileError(errorMessage);
            return EXIT_COMPILE_ERROR;

        } catch (CommandLine.ParameterException parameterException) {
            // Error while parsing command line arguments.
            if (args.length == 0) {
                // Display help if no arguments have been provided.
                displayHelp(cli, cliAnsi);
            } else {
                System.out.println(parameterException.getMessage());
            }
            return EXIT_USER_ERROR;

        } catch (IOException ioException) {
            // The IOException (hopefully) has a message, describing the problem. Display that.
            String exceptionMessage = (ioException.getMessage() != null)
                    ? ioException.getMessage()
                    : ioException.toString();

            System.err.println(exceptionMessage);
            return EXIT_IO_ERROR;

        } catch (Exception fatal) {
            // A fatal exception occurred, print it to the user.
            fatal.printStackTrace();
            return EXIT_FATAL;

        }
    }

    /**
     * A static helper method, calling {@link Application#execute(String[])}
     * on the provided arguments after setting up {@link Ansi} support.
     * After the {@link Application} returns, {@link Ansi} support is
     * cleaned up and {@link System#exit(int)} is called with the returned
     * exit code.
     *
     * @param args The command line arguments provided by the user.
     * @param app  The {@link Application} to run.
     */
    public static void runApplication(String[] args, Application app) {
        int exitCode;

        AnsiConsole.systemInstall();
        try {
            exitCode = app.execute(args);
        } finally {
            AnsiConsole.systemUninstall();
        }

        System.exit(exitCode);
    }

    /**
     * Reports a {@link ErrorMessage} by calling {@link ErrorMessage#toAnsi(Ansi)}
     * with an appropriate {@link Ansi} buffer.
     */
    public static void reportCompileError(ErrorMessage errorMessage) {
        Ansi outputBuffer = Ansi.ansi();
        errorMessage.toAnsi(outputBuffer);
        System.out.println(outputBuffer);
    }

    // Encapsulate all default options, so they appear as a single argument group in picocli.
    private static final class ApplicationOptions {
        @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true,
                description = "Display this help message and exit. " +
                        "If a backend is selected, its help page is displayed instead.")
        public boolean requestedHelp;


        @CommandLine.Option(names = "--version", versionHelp = true,
                description = "Displays information about this executables version and exits.")
        public boolean requestedVersionInfo;


        @CommandLine.Option(names = "--color", negatable = true,
                description = "Toggles whether printed warnings and errors should be colored or not. " +
                        "If no explicit option is given, this program tries to figure out automatically, " +
                        "if colored output is desired, depending on how this executable was called.")
        public Boolean coloredOutput = null; // Defaults to null (meaning auto-detect color).
    }
}
