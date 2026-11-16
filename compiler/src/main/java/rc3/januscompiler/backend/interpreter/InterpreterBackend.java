package rc3.januscompiler.backend.interpreter;

import rc3.januscompiler.Compiler;
import rc3.januscompiler.backend.Backend;
import rc3.januscompiler.backend.VariableAllocator;
import rc3.januscompiler.syntax.ProcedureDeclaration;
import rc3.januscompiler.syntax.Program;
import rc3.januscompiler.syntax.VariableDeclaration;
import rc3.januscompiler.table.Builtins;
import rc3.januscompiler.table.Environment;
import rc3.januscompiler.table.ParameterType;
import rc3.januscompiler.types.Type;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

public final class InterpreterBackend extends Backend {

    public InterpreterBackend() {
        super(new InterpreterAllocator());
    }

    @Override
    public void execute(Compiler compiler, Environment environment, Program program, PrintStream out) {
        if (compiler.dumpOptions.dumpIntermediate) {
            System.out.println("No intermediate code present. Exiting.");
            return;
        }

        Interpreter interpreter = new Interpreter(program, out);
        interpreter.run();

        if (compiler.printMain) {
            out.println();
            out.println("Variables at end of 'main' procedure:");
            for (VariableDeclaration localVariable : interpreter.mainProcedureDeclaration.variables) {
                Store variableValue = interpreter.currentStackFrame().get(localVariable.entry);
                out.printf("%s = %s\n", localVariable.name, variableValue);
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Janus Interpreter";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of(
                "This backend executes the Janus program rather than compiling it into an executable format. ");
    }


    /**
     * A primitive {@link VariableAllocator} implementation reserving one slot for each variable, since
     * variables are stored as Java objects in an array.
     */
    static class InterpreterAllocator extends VariableAllocator {

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
            return currentOffset + sizeOfAddress();
        }

        @Override
        public int initialLocalVariableOffset(ProcedureDeclaration procedureDeclaration) {
            return procedureDeclaration.entry.parameterAreaSize;
        }

        @Override
        public int applyVariableOffset(int currentOffset, VariableDeclaration variable) {
            variable.entry.offset = currentOffset;
            return currentOffset + sizeOf(variable.type);
        }

        @Override
        public int sizeOf(Type type) {
            // Every variable takes up 1 place in an array, regardless of its type.
            return 1;
        }

        @Override
        public int sizeOfAddress() {
            return 1;
        }
    }
}
