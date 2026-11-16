package rc3.januscompiler.commandline;

import picocli.CommandLine;
import rc3.januscompiler.backend.Backend;

import java.io.PrintStream;

/**
 * This class wraps the creation of a command line spec for picocli, which is used to display help and
 * usage options for different backends to the user.
 * <p>
 * Instances of this class only require a {@link Backend} instance to generate a help or usage page.
 */
public class BackendRenderer {
    public final Backend backend;
    private final CommandLine backendCli;

    public BackendRenderer(Backend backend) {
        this.backend = backend;
        this.backendCli = new CommandLine(backend);
        prepareCli();
    }

    public void showUsage(PrintStream printStream, CommandLine.Help.Ansi ansi) {
        backendCli.usage(printStream, ansi);
    }

    private void prepareCli() {
        CommandLine.Model.UsageMessageSpec usageMessageSpec = backendCli.getCommandSpec().usageMessage();

        usageMessageSpec.autoWidth(true);

        usageMessageSpec.customSynopsis(backend.getDisplayName());
        usageMessageSpec.synopsisHeading("");

        usageMessageSpec.descriptionHeading("%n");
        backend.getDescription().ifPresent(usageMessageSpec::description);

        usageMessageSpec.optionListHeading("%nBackend options:%n");
    }
}
