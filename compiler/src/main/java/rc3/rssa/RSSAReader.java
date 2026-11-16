package rc3.rssa;

import picocli.CommandLine;
import rc3.januscompiler.Compiler;
import rc3.lib.Timers;
import rc3.lib.messages.ErrorMessage;
import rc3.lib.parsing.Source;
import rc3.lib.parsing.WithCUP;
import rc3.lib.parsing.WithJFlex;
import rc3.rssa.instances.Program;
import rc3.rssa.parse.ParsedProgram;
import rc3.rssa.parse.Parser;
import rc3.rssa.parse.Scanner;
import rc3.rssa.parse.symbol.RSSASymbolFactory;
import rc3.rssa.pass.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class implements the RSSA fronted, which scans and parses RSSA code from an {@link #inputFile}.
 * It is inspired from the Janus Compiler {@link Compiler} class.
 */
public final class RSSAReader implements WithCUP, WithJFlex {

    public final RSSAReader.DumpOptions dumpOptions;
    private final Timers timers = new Timers();
    private final File inputFile;

    public RSSAReader(RSSAReader.DumpOptions dumpOptions, File inputFile) {
        this.dumpOptions = dumpOptions;
        this.inputFile = inputFile;
    }

    public Timers getTimers() {
        return timers;
    }

    public Optional<Program> read() throws IOException, ErrorMessage {
        try (Source source = Source.fromFileOrDefault(inputFile)) {
            final Scanner scanner = createScanner(Scanner::new, source);

            if (dumpOptions.dumpTokens) {
                // Show all tokens and exit.
                dumpTokens(scanner);
                return Optional.empty();
            }

            // Parse input.
            final Parser parser = new Parser(scanner, new RSSASymbolFactory());
            final ParsedProgram parsed = timers.time(Main.TIME_SYNTAX,
                    () -> (ParsedProgram) parse(parser));

            // Semantic analysis
            final Program program = timers.time(Main.TIME_SEMANTIC,
                    () -> runAnalysisOn(parsed));

            if (dumpOptions.dumpAst) {
                // Show the result of parsing and exit.
                Stream.concat(program.globalAnnotations().stream(), program.asListOfInstructions().stream())
                        .map(String::valueOf)
                        .forEach(System.out::println);
                return Optional.empty();
            }

            return Optional.of(program);
        }
    }

    private final ProceduresPass proceduresPass = new ProceduresPass();
    private final LabelPass labelPass = new LabelPass();
    private final List<Pass.Validation> passes = List.of(new CallPass(), new VariablePass());

    private Program runAnalysisOn(ParsedProgram parsed) {
        final var code = proceduresPass.andThen(labelPass).apply(parsed);
        final Program program = new Program(parsed.annotations(), code);

        for (Pass.Validation pass : passes) {
            pass.execute(program);
        }

        return program;
    }

    public static final class DumpOptions {
        @CommandLine.Option(names = "--dump-tokens",
                description = "Dumps the output of the lexical analysis. ")
        public boolean dumpTokens;
        @CommandLine.Option(names = "--dump-ast",
                description = "Dumps the output of the syntax analysis.")
        public boolean dumpAst;
    }
}


