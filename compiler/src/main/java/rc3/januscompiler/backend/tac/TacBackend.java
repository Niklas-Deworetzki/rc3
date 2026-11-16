package rc3.januscompiler.backend.tac;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.backend.AnnotationMode;
import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.backend.VariableAllocator;
import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.syntax.VariableDeclaration;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.tac.TacGenerator;
import rc3.januscompiler.tac.instances.Instruction;
import rc3.januscompiler.types.ArrayType;
import rc3.januscompiler.types.Type;
import rc3.lib.messages.ErrorMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import static picocli.CommandLine.Option;

/**
 * This {@link Backend} generates a single executable C file, that can be compiled by an external C compiler.
 */
public class TacBackend extends Backend {
    // The prelude is packed within the .jar file.
    private static final String PRELUDE_RESOURCE_PATH = "/tac/prelude.c";

    @Option(names = "debug",
            description = "Adds debug annotations to the generated C-code and disables all implicit checks.")
    public boolean debugBuild;

    @Option(names = "implicit-nil", negatable = true,
            description = "Enables implicit checks of empty stacks during pop or empty operations.")
    public boolean implicitNilChecks = false;
    @Option(names = "implicit-arithmetic", negatable = true,
            description = "Enables implicit checks of arithmetic errors such as division by zero.")
    public boolean implicitArithmeticChecks = true;

    @Option(names = "count-instructions",
            description = "Adds profiling instructions to the generated C code to measure the amount of executed instructions.")
    public boolean countExecutedInstructions = false;

    public TacBackend() {
        super(new CAllocator());
    }

    @Override
    protected void execute(Compiler compiler, Environment environment, Program program, PrintStream out) {
        CRuntimeLibrary cRuntimeLibrary = new CRuntimeLibrary(debugBuild, implicitNilChecks, implicitArithmeticChecks,
                countExecutedInstructions);
        CodeEmitter emitter = new CodeEmitter(out);

        // Determine annotation mode
        var annotationMode = debugBuild
                ? AnnotationMode.SOURCE_ANNOTATIONS
                : AnnotationMode.NO_ANNOTATIONS;

        // Generate list of Three Address Code instructions.
        List<Instruction> instructions = TacGenerator.generateCode(environment, cRuntimeLibrary, program, annotationMode);
        if (compiler.dumpOptions.dumpIntermediate) {
            instructions.forEach(System.out::println);
            return;
        }

        // Group list of instructions into C functions.
        List<CFunction> functions = new FunctionBuilder().buildFunctionsFrom(instructions);

        // Generate the C code.
        generateCCode(cRuntimeLibrary, emitter, functions, compiler.printMain);
    }

    protected void generateCCode(CRuntimeLibrary cRuntimeLibrary, CodeEmitter emitter,
                                 List<CFunction> functions, boolean printMain) {
        // Emit definitions required for the Janus program.
        cRuntimeLibrary.emitPrologue(emitter, functions);
        emitPrelude(emitter);

        // Emit all function prototypes, so they can be referenced in the procedure bodies.
        for (CFunction function : functions) {
            if (!function.isJanusMain()) {
                emitter.emitLine(function.getSignatureString() + ";");
            }
        }

        for (CFunction function : functions) {
            new CCodeGenerator(function, emitter, printMain, countExecutedInstructions).run();
        }
    }

    /**
     * Emits the prelude that is stored within the .jar file. If this operation fails, an
     * {@link ErrorMessage.InternalError} is thrown.
     *
     * @throws ErrorMessage.InternalError If the prelude can not be loaded or a write error occurs.
     */
    protected void emitPrelude(CodeEmitter emitter) throws ErrorMessage.InternalError {
        try (InputStream prelude = TacBackend.class.getResourceAsStream(PRELUDE_RESOURCE_PATH)) {
            if (prelude == null) {
                throw new ErrorMessage.InternalError(
                        String.format("Resource '%s' not found!", PRELUDE_RESOURCE_PATH));
            }

            prelude.transferTo(emitter.getRawOutput());
        } catch (IOException ioException) {
            throw new ErrorMessage.InternalError("Failed to emit Prelude!", ioException);
        }
    }

    @Override
    public String getDisplayName() {
        return "Three Address Code Backend";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of(
                "This backend translates the Janus code into Three Address Code (3AC) instructions, which are " +
                        "translated into C statements, to create an executable program.");
    }


    /**
     * A simple {@link VariableAllocator} implementation reserving one C variable for every encountered
     * Janus variable. Arrays require as many variables as there are elements in the array.
     */
    private static final class CAllocator extends VariableAllocator {

        @Override
        public void allocateBuiltinProcedureOffsets(Builtins.Procedure builtinProcedure, List<ParameterType> params) {
            for (int i = 0; i < params.size(); i++) {
                params.get(i).offset = i * sizeOfAddress();
            }
        }

        @Override
        public int initialParameterOffset(ProcedureDeclaration procedureDeclaration) {
            return 0;
        }

        @Override
        public int applyParameterOffset(int currentOffset, VariableDeclaration parameter) {
            parameter.entry.offset = currentOffset;
            return currentOffset + 1; // Parameters are just 1 reference.
        }

        @Override
        public int initialLocalVariableOffset(ProcedureDeclaration procedureDeclaration) {
            return 0;
        }

        @Override
        public int applyVariableOffset(int currentOffset, VariableDeclaration variable) {
            variable.entry.offset = currentOffset;
            return currentOffset + sizeOf(variable.type);
        }

        @Override
        public int sizeOf(Type type) {
            if (type instanceof ArrayType arrayType) {
                return arrayType.size;
            }
            return 1;
        }

        @Override
        public int sizeOfAddress() {
            return 1;
        }
    }
}
