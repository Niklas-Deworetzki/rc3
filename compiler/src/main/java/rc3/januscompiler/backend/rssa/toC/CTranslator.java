package rc3.januscompiler.backend.rssa.toC;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.Direction;
import rc3.januscompiler.backend.AnnotationMode;
import rc3.januscompiler.backend.rssa.RSSABackend;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.HeapView;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.instances.ConditionalEntry;
import rc3.rssa.instances.Program;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the translation of {@link Program RSSA programs} to C code.
 * <p>
 * The constructor of this class accepts general configuration for the translation, while the
 * translation itself is started via {@link CTranslator#emitCCode(Compiler, PrintStream, Program)}.
 */
public final class CTranslator {
    public final boolean measureRuntime;
    public final boolean countExecutedInstructions;
    public final AnnotationMode annotationMode;

    // The prelude is packed within the .jar file.
    private static final String PRELUDE_RESOURCE_PATH = "/rssa/prelude.c";

    public CTranslator(AnnotationMode sourceAnnotations, boolean measureRuntime, boolean countExecutedInstructions) {
        this.annotationMode = sourceAnnotations;
        this.measureRuntime = measureRuntime;
        this.countExecutedInstructions = countExecutedInstructions;
    }

    /**
     * Translates the given program under the present options.
     * <p>
     * The translation is a complete and executable C program that is written to the output stream passed
     * to this method.
     *
     * @param compiler    The {@link Compiler} under which this compilation happens.
     * @param globalOut   The output to which the C program is written to.
     * @param rssaProgram The {@link Program} that is translated to C.
     */
    public void emitCCode(Compiler compiler, PrintStream globalOut, Program rssaProgram) {
        try (PrintWriter out = new PrintWriter(globalOut)) { // PrintWriter is preferred to write Strings.
            List<CFunction> translatedFunctions = new ArrayList<>();
            for (ControlGraph rssaProcedure : rssaProgram.procedures().values()) {
                translatedFunctions.addAll(translateProcedure(rssaProcedure));
            }

            for (String requiredDefinition : generateRequiredDefinitions(rssaProgram, translatedFunctions)) {
                out.println(requiredDefinition);
            }

            out.println(getRuntimeLibrary());

            out.println("\n// Prototypes for used functions.");
            for (CFunction cFunction : translatedFunctions) {
                out.println(cFunction.getPrototype());
            }

            out.println("\n// All function bodies.");

            for (CFunction cFunction : translatedFunctions) {
                cFunction.writeImplementation(out);
            }

            out.println("\n// Setup functions allocating variables.");
            CBootstrap.generateBootstrapCode(rssaProgram, compiler.printMain, out);
        }
    }

    /**
     * Loads the runtime library for RSSA programs in C into a {@link String} and returns it.
     *
     * @throws ErrorMessage if it fails to load the resource.
     */
    public static String getRuntimeLibrary() {
        final String preludePath = PRELUDE_RESOURCE_PATH;
        //noinspection ConstantConditions Intellij reports the argument to InputStreamReader as possible null, which is handled using NPE.
        try (Reader prelude = new InputStreamReader(CTranslator.class.getResourceAsStream(preludePath))) {
            StringWriter preludeText = new StringWriter();
            prelude.transferTo(preludeText);

            return preludeText.toString();

        } catch (IOException ioException) {
            throw new ErrorMessage.InternalError("Failed to emit Prelude!", ioException);
        } catch (NullPointerException nullPointerException) {
            throw new ErrorMessage.InternalError(String.format("Resource '%s' not found!", preludePath));
        }
    }


    /**
     * Generates preprocessor definitions.
     * <p>
     * The generated definitions include:
     * <ul>
     *  <li><code>RSSA_HEAPSIZE</code> the total available heap.</li>
     *  <li><code>STOP_RUNTIME</code> if we want to stop the runtime.</li>
     *  <li><code>COUNT_INSTRUCTIONS</code> and <code>PROCEDURE_COUNT</code> if the executed instructions
     *  should be counted during execution.</li>
     * </ul>
     */
    public List<String> generateRequiredDefinitions(Program rssaProgram, List<CFunction> translatedProgram) {
        List<String> defines = new ArrayList<>();

        int heapSize = rssaProgram.findAnnotation("Heap")
                .<HeapView>map(Annotation::getView)
                .map(heapView -> heapView.getSize().value)
                .orElse(RSSABackend.DEFAULT_HEAP_SIZE);
        defines.add(String.format("#define RSSA_HEAPSIZE (%d)", heapSize));

        if (measureRuntime) {
            defines.add("#define STOP_RUNTIME");
        }

        if (countExecutedInstructions) {
            defines.add("#define COUNT_INSTRUCTIONS");
            defines.add(String.format("#define PROCEDURE_COUNT (%d)", translatedProgram.size()));
        }

        return defines;
    }

    /**
     * Creates all required {@link CFunction} instances to translate all
     * {@link BasicBlock BasicBlocks} of a {@link ControlGraph} in all
     * directions and variants.
     */
    public List<CFunction> translateProcedure(ControlGraph procedureGraph) {
        List<CFunction> functions = new ArrayList<>();

        for (BasicBlock block : procedureGraph) {
            functions.addAll(translateBlock(annotationMode, Direction.FORWARD, procedureGraph, block));
        }
        var reversedGraph = procedureGraph.reversed();
        for (BasicBlock block : reversedGraph) {
            functions.addAll(translateBlock(annotationMode, Direction.BACKWARD, reversedGraph, block));
        }

        return functions;
    }

    private List<CFunction> translateBlock(AnnotationMode annotationMode, Direction direction,
                                           ControlGraph graph, BasicBlock block) {
        List<CFunction> result = new ArrayList<>();

        if (block.entryPoint() instanceof ConditionalEntry) {
            // Conditional entry must be translated with both entry options.
            result.add(new CTranslationVisitor(graph, block, direction,
                    annotationMode, countExecutedInstructions, true).toCFunction());
            result.add(new CTranslationVisitor(graph, block, direction,
                    annotationMode, countExecutedInstructions, false).toCFunction());
        } else {
            result.add(new CTranslationVisitor(graph, block, direction,
                    annotationMode, countExecutedInstructions).toCFunction());
        }

        return result;
    }

}
