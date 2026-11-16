package rc3.januscompiler.backend.tac;

import rc3.januscompiler.backend.NameResolver;
import rc3.januscompiler.tac.RuntimeLibrary;
import rc3.januscompiler.tac.StackImplementation;
import rc3.januscompiler.tac.TacGenerator;
import rc3.januscompiler.tac.instances.Constant;
import rc3.januscompiler.tac.instances.Instruction;
import rc3.januscompiler.types.ArrayType;

import java.util.List;

/**
 * This class acts as a {@link RuntimeLibrary} for the {@link TacGenerator} to generate
 * {@link Instruction Instructions} that are translatable to C code.
 * <p>
 * This class supports the generation of implicit checks for division by zero and implicit nil checks
 * on stack operations.
 * </p>
 */
public class CRuntimeLibrary implements RuntimeLibrary {

    // Defines enable the implicit checks in the generated C code.
    private static final String DEFINE_IMPLICIT_NIL_CHECK = "#define IMPLICIT_NIL";
    private static final String DEFINE_IMPLICIT_ARITHMETIC_CHECK = "#define IMPLICIT_ARITHMETIC";
    private static final String DEFINE_COUNT_EXECUTED_INSTRUCTIONS = "#define COUNT_INSTRUCTIONS";
    private static final String DEFINE_PROCEDURE_COUNT = "#define PROCEDURE_COUNT (%d)";

    private final boolean implicitArithmeticChecks;
    private final boolean implicitNilChecks;
    private final boolean countExecutedInstructions;

    private final StackImplementation stackImplementation;

    public CRuntimeLibrary(boolean debugBuild, boolean implicitNilChecks, boolean implicitArithmeticChecks,
                           boolean countExecutedInstructions) {
        if (debugBuild || countExecutedInstructions) {
            this.implicitArithmeticChecks = false;
            this.implicitNilChecks = false;
        } else {
            this.implicitNilChecks = implicitNilChecks;
            this.implicitArithmeticChecks = implicitArithmeticChecks;
        }
        this.countExecutedInstructions = countExecutedInstructions;

        this.stackImplementation = new ReferenceCountingStackImplementation(this.implicitNilChecks);
    }


    /**
     * Emits <code>#define</code> macros to enable implicit checks.
     */
    public void emitPrologue(CodeEmitter emitter, List<CFunction> cFunctions) {
        if (implicitArithmeticChecks) {
            emitter.emitLine(DEFINE_IMPLICIT_ARITHMETIC_CHECK);
        }
        if (implicitNilChecks) {
            emitter.emitLine(DEFINE_IMPLICIT_NIL_CHECK);
        }
        if (countExecutedInstructions) {
            emitter.emitLine(DEFINE_COUNT_EXECUTED_INSTRUCTIONS);
            emitter.emitLine(DEFINE_PROCEDURE_COUNT, cFunctions.size());
        }
    }


    @Override
    public boolean checkArithmetic() {
        return !implicitArithmeticChecks;
    }

    @Override
    public Constant elementSize(ArrayType arrayType) {
        return new Constant(1);
    }

    private static boolean isReservedPreludeName(String name) {
        if (name.startsWith("builtin") || name.startsWith("printVariable")) {
            char endOfString = name.charAt(name.length() - 1);
            return !Character.isDigit(endOfString);
        }
        return "error".equals(name);
    }

    @Override
    public NameResolver getNameResolver() {
        return new CNameResolver(CRuntimeLibrary::isReservedPreludeName);
    }

    @Override
    public StackImplementation getStackImplementation() {
        return stackImplementation;
    }
}
