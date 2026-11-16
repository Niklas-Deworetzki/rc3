package rc3.rssa.vm;

import rc3.januscompiler.Direction;
import rc3.januscompiler.syntax.AssignStatement;
import rc3.januscompiler.syntax.visitor.TreeVisitor;
import rc3.lib.messages.WarningMessage;
import rc3.lib.parsing.Position;
import rc3.rssa.annotations.*;
import rc3.rssa.annotations.views.*;
import rc3.rssa.instances.*;
import rc3.rssa.parse.ParsedProgram;
import rc3.rssa.pass.LabelEntry;
import rc3.rssa.pass.LabelTable;
import rc3.rssa.pass.StackAllocation;
import rc3.rssa.visitor.DoNothingRSSAVisitor;
import rc3.rssa.visitor.VoidRSSAVisitor;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements a virtual machine for  reversible SSA (RSSA) code.
 * <p>
 * To execute a {@link ParsedProgram}, a new {@link RSSAVM} instance has to be created. The constructor of this
 * class accepts the program as a parameter.<br/>
 * The execution is started by invoking the {@link RSSAVM#run()} method, which will bootstrap
 * the initial stack frame, executes global annotations, if any, and subsequently will execute the statements
 * of the main subroutine.<br/>
 * <b>The executed program must be semantically valid. Otherwise runtime exceptions may occur.</b>
 * </p>
 * <p>
 * The interpretation is split into two parts, which are implemented in distinct {@link TreeVisitor} instances.<br/>
 * To execute {@link Instruction Instructions}, the {@link RSSAExecutor} class is used which represent forward and
 * backward execution.<br/>
 * {@link Value Values} evaluated using the {@link EvaluationVisitor} which again is used in both forward and
 * backward execution.
 * The {@link AnnotationExecutor} executes different {@link Annotation Annotations}.
 * </p>
 * <p>
 * If the executed program performs an undefined operation, a {@link FailedAssertion} is thrown. It is populated
 * with the stack trace of the executed program and may be used accordingly.
 * </p>
 *
 * @see RSSAExecutor
 * @see EvaluationVisitor
 */
public class RSSAVM implements Runnable {
    // Default values
    public static final int DEFAULT_HEAP_SIZE = 102400;

    /**
     * This enum implements different states of the virtual machine.
     */
    public enum State {
        SETUP("setup"),
        STOPPED("paused"),
        FINISHED("finished");

        private final String name;

        State(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return String.format("The program execution is %s.", name);
        }

        public boolean isRunning() {
            return this == SETUP || this == STOPPED;
        }
    }

    // Instruction pointer
    public int ip = -3;

    // Instructions and (global) Annotations
    private final List<Instruction> code;
    private final List<Annotation> annotations;

    // Lookup Tables
    private final Map<String, LabelEntry> labelTable;

    // Program Stacks
    private final Deque<Integer> argStack = new ArrayDeque<>();
    private final Deque<StackFrame> callStack = new ArrayDeque<>();

    // Program Memory
    private int[] memory;

    // Output options
    private PrintStream printStream;
    private boolean shouldPrintMain;
    private boolean shouldCountInstructions;

    // Visitor and Executor instances
    private final RSSAExecutor executor;
    private final EvaluationVisitor evaluationVisitor = new EvaluationVisitor();
    private final AnnotationExecutor annotationExecutor = new AnnotationExecutor();

    // Debug information
    private final Map<String, Long> instructionsCountFw = new HashMap<>();
    private final Map<String, Long> instructionsCountBw = new HashMap<>();
    private String mainOutput = "";

    // Debugger
    private boolean debug;
    private final boolean[] breakpoints;
    private boolean reverseIsForbidden;
    private String previousInstructionString; // previous formatted instruction (used for debugging)

    // Others
    private int lastIp; // last (previous) ip (used for debugging and exceptions)
    private State state = State.FINISHED;
    private final String mainEntry;

    // Diagnostic
    public long executedInstructions;

    public RSSAVM(Program program, Direction initialDirection, PrintStream output,
                  boolean shouldPrintMain, boolean shouldCountInstructions) {
        this.code = program.asListOfInstructions();
        this.annotations = program.globalAnnotations();

        this.breakpoints = new boolean[this.code.size()];
        this.mainEntry = program.getEntryPoint();
        this.executor = new RSSAExecutor(initialDirection);

        this.printStream = output;
        this.shouldPrintMain = shouldPrintMain;
        this.shouldCountInstructions = shouldCountInstructions;

        StackAllocation.allocateStackFrames(program);
        this.labelTable = new LabelTable(program).build();
    }


    @Override
    public void run() {
        setup();
        // execution loop
        execute();
    }

    public void execute() {
        do {
            if (breakpoints[ip]) { // stop at breakpoint
                state = State.STOPPED;
                debugPrintf("| Breakpoint at %s%n", formatNextInstruction());
                return;
            } else {
                step(false);
            }
        } while (!callStack.isEmpty());
    }

    public void step(boolean debugOutput) {
        Instruction instr = code.get(ip);
        if (executor.isForward()) {
            lastIp = ip++;
        } else {
            instr = instr.invert();
            lastIp = ip--;
        }
        // update (and print) debug output
        executedInstructions++;
        if (debug || shouldCountInstructions) {
            updateInstructionsCount(true);
        }
        if (debug) {
            previousInstructionString = formatCurrentInstruction(instr);
        }
        if (debugOutput) {
            debugPrintf("| %s%n", previousInstructionString);
        }
        // execute instruction
        try {
            annotationExecutor.executeLocal(instr.getAnnotations()); // execute local annotations after instruction
            instr.accept(executor);
        } catch (ArrayIndexOutOfBoundsException e) {
            throwRSSAException(instr, e);
            RSSARuntimeException runtimeException = new RSSARuntimeException(String.format("Memory index is out of bounds (%d)!", memory.length));
            setStackTrace(runtimeException, currentInstruction());
            state = State.FINISHED;
            throw runtimeException;
        }
        // print main output
        if (state == State.FINISHED) {
            mainOutput += instructionsCountOutput();
            printStream.print(mainOutput);
        }
    }

    private void setup() {
        if (debug) {  // Restore default values / clear everything for debugger
            this.argStack.clear();
            this.callStack.clear();
            instructionsCountFw.clear();
            instructionsCountBw.clear();
            this.executor.parseReturnArgs = false;
            this.reverseIsForbidden = true;
            this.previousInstructionString = null;
            this.executedInstructions = 0;
        }
        this.memory = new int[DEFAULT_HEAP_SIZE];
        annotationExecutor.executeGlobal(annotations); // execute global annotations if any
        initMainFrame(); //setup initial stack frame and ip
        state = State.SETUP;
        //reverseIsForbidden = true;
    }

    private void initMainFrame() {
        lastIp = ip = getLabelAddr(mainEntry);
        executor.lastLabel = mainEntry;
        Instruction main = code.get(ip);
        var atoms = ((ControlInstruction) main).getParameters();
        atoms.forEach(this::initMainAtom);
        callStack.push(new StackFrame(executor.direction, executor.direction,
                getFrameSize(mainEntry), -3, getSourcePosition(main), mainEntry, mainEntry));
    }

    private void initMainAtom(Atom atom) {
        argStack.push(atom.findAnnotation("Param").<ParamView>map(Annotation::getView)
                .map(ParamView::getValue).map(Constant::getValue)
                .orElse(atom instanceof Constant ? ((Constant) atom).value : 0));
    }

    public int getLabelAddr(String label) {
        return getLabelAddr(label, executor.direction);
    }

    public int getLabelAddr(String label, Direction direction) {
        return labelTable.get(label).getLabelAddr(direction);
    }

    private int getFrameSize(String label) {
        return labelTable.get(label).getFrameSize();
    }

    ////////////////////////////////////
    //    RSSA EXCEPTIONS
    ////////////////////////////////////

    protected void validateAssertion(boolean assertion, String fmt, Object... args)
            throws FailedAssertion {
        if (!assertion) {
            FailedAssertion failedAssertion = new FailedAssertion(String.format(fmt, args));
            setStackTrace(failedAssertion, currentInstruction());
            state = State.FINISHED;
            throw failedAssertion;
        }
    }

    protected void throwRSSAException(Instruction instr, Exception exception) throws RuntimeException {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            message = exception.toString();
        }
        RSSARuntimeException runtimeException = new RSSARuntimeException(message);
        setStackTrace(runtimeException, instr);
        state = State.FINISHED;
        throw runtimeException;
    }

    private void setStackTrace(RSSARuntimeException exception, Annotatable annotatable) {
        Optional<Position> position = getSourcePosition(annotatable);
        if (position.isEmpty()) {
            exception.setStackTrace(StackFrame.buildStackTrace(annotatable, callStack));
        } else {
            exception.setStackTrace(StackFrame.buildStackTrace(position, callStack));
        }
    }

    /**
     * Uses an {@link RSSAVM.EvaluationVisitor} instance to evaluate the given {@link Value}.
     *
     * @param value The value to evaluate
     * @return The evaluated result of the given node as an int, since only integers exist in RSSA.
     */
    int evaluate(Value value) {
        return value.accept(evaluationVisitor);
    }

    ////////////////////////////////////
    //    GETTERS AND SETTERS
    ////////////////////////////////////

    /**
     * Enables or disables a breakpoint for an instruction referenced via {@link #code a list of instructions}.
     *
     * @param i     The index of the instruction in {@link #code}
     * @param value The state of the breakpoint, true is enabled, false disabled
     */
    public void setBreakpoint(int i, boolean value) {
        breakpoints[i] = value;
    }

    public State getState() {
        return state;
    }

    public Instruction getInstruction(int idx) {
        return executor.isForward() ? code.get(idx) : code.get(idx).invert();
    }

    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    public String getMainEntry() {
        return mainEntry;
    }

    public void setShouldPrintMain(boolean shouldPrintMain) {
        this.shouldPrintMain = shouldPrintMain;
    }

    public boolean getShouldPrintMain() {
        return this.shouldPrintMain;
    }

    public void setShouldCountInstructions(boolean shouldCountInstructions) {
        this.shouldCountInstructions = shouldCountInstructions;
    }

    public boolean getShouldCountInstructions() {
        return shouldCountInstructions;
    }

    public boolean getReverseIsForbidden() {
        return this.reverseIsForbidden;
    }

    public boolean[] getBreakpoints() {
        return breakpoints;
    }

    public Direction getDirection() {
        return executor.direction;
    }

    public void setDirection(Direction direction) {
        executor.direction = direction;
    }

    public Deque<StackFrame> getCallStack() {
        return callStack;
    }

    public String getPreviousInstructionString() {
        return previousInstructionString;
    }

    public boolean getParseReturnArgs() {
        return executor.parseReturnArgs;
    }

    ////////////////////////////////////
    //    OTHER HELP METHODS
    ////////////////////////////////////

    public StackFrame currentStackFrame() {
        return callStack.peek();
    }

    private Optional<Position> getSourcePosition(Annotatable annotatable) {
        return Instruction.findPosition(annotatable);
    }

    /**
     * @return the last {@link Instruction} before the instruction pointer ({@link #ip}) was updated
     */
    public Instruction currentInstruction() {
        return executor.isForward() ? code.get(lastIp) : code.get(lastIp).invert();
    }

    /**
     * Swaps the {@link #ip instruction pointer} with the last known value {@link #lastIp}
     */
    public void swapIp() {
        ip ^= lastIp;
        lastIp ^= ip;
        ip ^= lastIp;
    }

    private void debugPrintf(String fmt, Object... args) {
        if (debug) {
            printStream.printf(fmt, args);
        }
    }

    public void enableDebugMode() {
        debug = true;
    }

    public void reverseDirection() {
        this.executor.direction = this.executor.direction.invert();
    }

    public String instructionsCountOutput() {
        StringBuilder sb = new StringBuilder();
        if (shouldCountInstructions) {
            sb.append("\nListing called subroutines/labels and number of executed instructions:\n");
            instructionsCountFw.forEach((k, v) -> sb.append(String.format("- fw.%s: %s%n", k, v)));
            instructionsCountBw.forEach((k, v) -> sb.append(String.format("- bw.%s: %s%n", k, v)));
            sb.append("Total executed instructions: ")
                    .append(executedInstructions).append("\n\n");
        }
        return sb.toString();
    }

    private void updateInstructionsCount(boolean increment) {
        var counter = executor.isForward() ? instructionsCountFw : instructionsCountBw;
        counter.put(executor.lastLabel, counter.getOrDefault(executor.lastLabel, 0L) + (increment ? +1 : -1));
    }

    public String formatNextInstruction() {
        var instr = getInstruction(ip);
        return String.format("#%03d\t%s%s", ip, instr, instr instanceof CallInstruction ?
                " (" + (executor.parseReturnArgs ? "restoring" : "passing") + ")" : "");
    }

    public String formatCurrentInstruction(Instruction instr) {
        var x = getInstruction(lastIp);
        return String.format("#%03d\t%s%s", lastIp, instr, instr instanceof CallInstruction ?
                " (" + (executor.parseReturnArgs ? "restoring" : "passing") + ")" : "");
    }

    /**
     * This executor class implements the execution of RSSA {@link Instruction Instructions} with visitor pattern.
     */
    public class RSSAExecutor extends VoidRSSAVisitor {
        public Direction direction;

        private String lastLabel;
        private boolean parseReturnArgs = false;

        public RSSAExecutor(Direction direction) {
            this.direction = direction;
        }

        /**
         * This method implements argument restoring from the last instruction. <br>
         * Every time an argument is restored it is popped from the arguments stack and gets created if it is a
         * instance of {@link Variable}.
         *
         * @param atoms The list of atoms that should be created (if variables) with the values on the arguments stack.
         */
        private void restoreAndCreate(List<Atom> atoms) {
            int passed = argStack.size();
            int restored = atoms.size();
            validateAssertion(atoms.size() == argStack.size(),
                    "%s atoms were expected (%d) than were %s (%d).",
                    restored > passed ? "More" : "Less", restored, parseReturnArgs ? "returned" : "passed", passed);
            for (int i = restored - 1; i >= 0; i--) {
                createIfVariableElseAssert(atoms.get(i), new Constant(argStack.pop()));
            }
            validateAssertion(argStack.isEmpty(), // TODO: unnecessary???
                    "The number of atoms that were passed does not match the number of atoms that were restored.");
        }

        /**
         * This method implements argument passing to next instructions. <br>
         * Every time an argument is passed it is pushed on the on an {@link Stack} and gets destroyed if it is an
         * instance of {@link Variable}.
         *
         * @param args The list of {@link Atom}s to pass as arguments to the next {@link Instruction} by pushing their
         *             values on the arguments stack and destroying them.
         */
        private void passAndDestroy(List<Atom> args) {
            validateAssertion(argStack.isEmpty(), // TODO: unnecessary???
                    "There are less arguments restored than passed.");
            args.forEach(atom -> {
                argStack.push(evaluate(atom));
                destroyIfVariable(atom);  // destruction at end not explicitly in Mogensen
            });
        }

        /**
         * This method implements rssa specific variable creation behavior.
         * If the atom target is a variable it gets created with the value of the source atom, if not,
         * then a validation will be performed to assert that the value of target is equals the value of source.
         * If the source atom is a variable it will be destroyed afterwards.
         *
         * @param target The atom to create.
         * @param source The atom which value will be assigned to the target.
         *               This atom gets destroyed if it is an variable.
         */
        private void createIfVariableElseAssert(Atom target, Atom source) {
            if (target instanceof Variable variable) {
                currentStackFrame().enter(variable, evaluate(source));
                // destroy source if source is a variable
                destroyIfVariable(source);
            } else { // instance of Constant
                int t = evaluate(target);
                int s = evaluate(source);
                validateAssertion(t == s, "Value %d is not equal target %d.", s, t);
            }
        }

        /**
         * This method implements rssa specific variable destruction behavior.
         * The atom will be destroyed if it is an variable, otherwise nothing will happen.
         *
         * @param atom The atom to destroy.
         */
        private void destroyIfVariable(Atom atom) {
            if (atom instanceof Variable variable) {
                currentStackFrame().exit(variable);
            } // Mogensen: If a constant is used where a variable would be destroyed, no variable is destroyed.
        }

        private boolean isTrue(int i) {
            return i != 0;
        }

        private boolean isFalse(int i) {
            return i == 0;
        }

        private boolean isForward() {
            return direction.isForward();
        }

        private boolean isBackward() {
            return !direction.isForward();
        }

        @Override
        protected void visitVoid(ArithmeticAssignment arithmeticInstruction) {
            var target = arithmeticInstruction.target;
            var value = new Constant(evaluationVisitor.evaluateAssignment(arithmeticInstruction.source,
                    arithmeticInstruction.getValue(), arithmeticInstruction.operator));
            destroyIfVariable(arithmeticInstruction.source);
            createIfVariableElseAssert(target, value);
            evaluate(arithmeticInstruction.getValue());
        }

        @Override
        protected void visitVoid(BeginInstruction beginInstruction) {
            // get arguments and create variables if needed
            //callStack.push(nextCallerFrame);//currentStackFrame().getCallerStackFrame());
            restoreAndCreate(beginInstruction.getParameters());
            reverseIsForbidden = parseReturnArgs = false;
            direction = currentStackFrame().calleeDirection; // TODO why again for debugger?
        }

        @Override
        protected void visitVoid(BinaryOperand binaryOperand) {
            // BinaryOperand is handled by EvaluationVisitor
        }

        @Override
        protected void visitVoid(CallInstruction callInstruction) {
            if (parseReturnArgs) {
                callStack.pop();
                // restore arguments
                restoreAndCreate(callInstruction.outputList);
                // because the double execution of a call statement to parse the return values should not count:
                if (debug || shouldCountInstructions) {
                    updateInstructionsCount(false);
                }
                executedInstructions--;
                /* reverseIsForbidden = */
                parseReturnArgs = false;
            } else {
                /*reverseIsForbidden = */
                parseReturnArgs = true;
                // change execution direction
                var oldDirection = this.direction;
                this.direction = callInstruction.direction;
                // pass arguments and destroy variables
                passAndDestroy(callInstruction.inputList);
                // create and save new stack frame
                var oldIp = lastIp; // ip was already in-/decreased
                callStack.push(new StackFrame(callInstruction.direction, oldDirection,
                        getFrameSize(callInstruction.procedure), oldIp, getSourcePosition(callInstruction),
                        callInstruction.procedure, lastLabel));
                // update ip to index of next label
                lastLabel = callInstruction.procedure;
                ip = getLabelAddr(lastLabel);
            }
        }

        @Override
        protected void visitVoid(ConditionalEntry conditionalEntry) {
            // get arguments and create variables if needed
            restoreAndCreate(conditionalEntry.getParameters());
            // evaluate condition and check type of condition
            var condition = isTrue(evaluate(conditionalEntry.getCondition()));
            // check if jump to label was allowed else throw error
            String label = condition ? conditionalEntry.trueLabel : conditionalEntry.falseLabel;
            validateAssertion(label.equals(lastLabel),
                    "Condition should be %b when entered through jump to label '%s' but it is not.",
                    !condition, lastLabel);
        }


        @Override
        protected void visitVoid(ConditionalExit conditionalExit) {
            // evaluate condition and check type of condition
            var evaluation = isTrue(evaluate(conditionalExit.getCondition()));
            // pass arguments and destroy variables
            passAndDestroy(conditionalExit.getParameters());
            // set ip to next label (with arguments)
            lastLabel = evaluation ? conditionalExit.trueLabel : conditionalExit.falseLabel;
            ip = getLabelAddr(lastLabel);
        }

        @Override
        protected void visitVoid(Constant constant) {
            // Constant is handled by EvaluationVisitor
        }

        @Override
        protected void visitVoid(EndInstruction endInstruction) {
            if (callStack.size() == 1) {
                // end of main procedure
                if (shouldPrintMain) {
                    mainOutput = generateEndOutput(endInstruction.getParameters());
                } else {
                    debugPrintf("Program finished.%n");
                }
                passAndDestroy(endInstruction.getParameters());
                state = State.FINISHED;
                StackFrame frame = callStack.pop();
                validateAssertion(frame.allDestroyed(),
                        "At end of subroutine '%s.%s' not all variables were destroyed.",
                        direction, endInstruction.label);
                reverseIsForbidden = parseReturnArgs = true;
                return;
            }
            // pass arguments and destroy variables
            /*reverseIsForbidden = */
            parseReturnArgs = true;
            passAndDestroy(endInstruction.getParameters());
            // pop frame
            StackFrame frame = callStack.getFirst();
            validateAssertion(frame.allDestroyed(),
                    "At end of subroutine '%s.%s' not all variables were destroyed.",
                    direction, endInstruction.label);
            executor.direction = frame.callerDirection;
            lastLabel = frame.callerLabel;
            ip = frame.oldIp;
        }

        private String generateEndOutput(Collection<Atom> atoms) {
            StringBuilder sb = new StringBuilder(
                    String.format("%nVariables at end of '%s.%s' subroutine:%n", direction, mainEntry));
            for (Atom atom : atoms) {
                sb.append(generateVariableOutput(atom));
            }
            return sb.toString();
        }

        private String generateVariableOutput(Atom atom) {
            String defaultName = String.format("%s = %s%n", atom instanceof Variable ?
                    ((Variable) atom).getRawName() : String.valueOf(evaluate(atom)), evaluate(atom));
            return atom.findAnnotation("Param").<ParamView>map(Annotation::getView)
                    .map(paramView -> annotationExecutor.executeParam(paramView, atom)).orElse(defaultName);
        }

        @Override
        protected void visitVoid(MemoryAccess memoryAccess) {
            // MemoryAccess is handled by EvaluationVisitor
        }

        @Override
        protected void visitVoid(MemoryAssignment memoryAssignment) {
            // evaluate index
            int index = evaluate(memoryAssignment.target.index);
            // assign value to memory at index
            memory[index] = evaluationVisitor.evaluateAssignment(memoryAssignment.target,
                    memoryAssignment.getValue(), memoryAssignment.operator);

        }

        @Override
        protected void visitVoid(MemoryInterchangeInstruction memoryInterchangeInstruction) {
            // MemoryInterchangeInstruction x := M[y] := z
            // evaluate z and destroy if variable
            int rightValue = evaluate(memoryInterchangeInstruction.right);
            destroyIfVariable(memoryInterchangeInstruction.right);
            // evaluate index y
            int index = evaluate(memoryInterchangeInstruction.memoryAccess.index);
            // evaluate memory at index y
            int memoryValue = evaluate(memoryInterchangeInstruction.memoryAccess);
            // assign value of z to memory at y
            memory[index] = rightValue;
            // create or assert x with saved value of memory
            createIfVariableElseAssert(memoryInterchangeInstruction.left, new Constant(memoryValue));

        }

        @Override
        protected void visitVoid(MemorySwapInstruction memorySwapInstruction) {
            // fetch left value
            int leftValue = evaluate(memorySwapInstruction.left);
            // save right value to memory at left index
            memory[evaluate(memorySwapInstruction.left.index)] = evaluate(memorySwapInstruction.right);
            // save left value to memory at right index
            memory[evaluate(memorySwapInstruction.right.index)] = leftValue;

        }

        @Override
        protected void visitVoid(SwapInstruction swapInstruction) {
            // SwapInstruction x, y := z, w
            // create x and destroy z or test if x = z
            createIfVariableElseAssert(swapInstruction.targetLeft, swapInstruction.sourceLeft);
            // create y and destroy w or test if y = w
            createIfVariableElseAssert(swapInstruction.targetRight, swapInstruction.sourceRight);
        }

        @Override
        protected void visitVoid(UnconditionalEntry unconditionalEntry) {
            // get arguments and create variables if needed
            restoreAndCreate(unconditionalEntry.getParameters());
        }

        @Override
        protected void visitVoid(UnconditionalExit unconditionalExit) {
            // set ip to next label (with arguments)
            passAndDestroy(unconditionalExit.getParameters());
            lastLabel = unconditionalExit.label;
            ip = getLabelAddr(lastLabel);
        }

        @Override
        protected void visitVoid(Variable variable) {
            // Variable is handled by EvaluationVisitor
        }
    }

    /**
     *
     */
    private class EvaluationVisitor extends DoNothingRSSAVisitor<Integer> {

        private int encodeBoolean(boolean b) {
            return b ? 1 : 0;
        }

        @Override
        public Integer visit(BinaryOperand binaryOperand) {
            int left = evaluate(binaryOperand.left);
            int right = evaluate(binaryOperand.right);

            if (binaryOperand.operator == BinaryOperand.BinaryOperator.DIV ||
                    binaryOperand.operator == BinaryOperand.BinaryOperator.MOD) {
                validateAssertion(right != 0, "Division by 0!");
            }

            if (binaryOperand.operator.isArithmetic()) {
                return binaryOperand.operator.evaluateConstant(left, right).orElseThrow();
            } else {
                return encodeBoolean(binaryOperand.operator.compareConstant(left, right));
            }
        }

        @Override
        public Integer visit(Constant constant) {
            return constant.value;
        }

        @Override
        public Integer visit(MemoryAccess memoryAccess) {
            memoryAccess.findAnnotation("InBounds")
                    .ifPresent(annotation -> annotationExecutor.executeInBounds(annotation, memoryAccess.index));
            int index = evaluate(memoryAccess.index);
            return memory[index];

        }

        @Override
        public Integer visit(Variable variable) {
            return currentStackFrame().get(variable);
        }

        Integer evaluateAssignment(Value left, Value right, AssignStatement.ModificationOperator operator) {
            int leftValue = evaluate(left);
            int rightValue = evaluate(right);
            return operator.evaluateConstant(leftValue, rightValue);
        }

    }

    private class AnnotationExecutor {
        protected void executeGlobal(Annotation annotation) {
            switch (annotation.getIdentifier()) {
                case "Heap":
                    HeapView heapView = annotation.getView();
                    memory = new int[evaluate(heapView.getSize())];
                    return;
                case "Memory":
                    MemoryView memView = annotation.getView();
                    memory[evaluate(memView.getAdress())] = evaluate(memView.getValue());
                    return;
                case "Entry":
                    // not executed here
                    return;
                default:
                    printStream.printf("WARNING: Annotation '%s' cannot be executed globally or is unknown.\n", annotation);
            }
        }

        protected void executeGlobal(Collection<Annotation> annotations) {
            annotations.forEach(this::executeGlobal);
        }

        protected void executeLocal(Annotation annotation) {
            switch (annotation.getIdentifier()) {
                case "Allocation":
                    AllocationView allocView = annotation.getView();
                    int heapUsage = evaluate(allocView.heapUsage());
                    int updatedHeapSize = heapUsage + evaluate(allocView.requestSize());
                    validateAssertion(updatedHeapSize <= memory.length && updatedHeapSize >= 0,
                            "The requested heap size (%d) exceeds the free heap capacity (%d)",
                            allocView.requestSize().value, memory.length - heapUsage);
                    return;
                case "Source":
                    // not executed here
                    return;
                default:
                    printStream.printf("WARNING: Annotation '%s' cannot be executed locally or is unknown.\n", annotation);
            }
        }

        protected void executeLocal(Collection<Annotation> annotations) {
            annotations.forEach(this::executeLocal);
        }

        /**
         * This method executes an @InBounds Annotation and checks if used index exceeds some boundaries.
         *
         * @param annotation The InBounds Annotation to execute.
         * @param atom       The atom where the annotation is used.
         */
        protected void executeInBounds(Annotation annotation, Atom atom) {
            InBoundsView inBoundsView = annotation.getView();
            int index = evaluate(atom);
            int base = evaluate((inBoundsView.getBaseAddress()));
            int size = evaluate(inBoundsView.getSize());
            validateAssertion(index >= base && index < base + size,
                    "Memory index (%d) is out of bounds for base (%d) and size (%d)!", index, base, size);
        }

        protected String executeParam(ParamView paramView, Atom atom) {
            if (!paramView.isHidden()) {
                var s = String.format("%s = ", paramView.getName().orElse(
                        atom instanceof Variable variable ? variable.getRawName()
                                : String.valueOf(evaluate(atom))));
                switch (paramView.getType()) {
                    case ARRAY:
                        s += generateArrayOutput(paramView, atom);
                        break;
                    case STACK:
                        s += generateStackOutput(paramView, atom);
                        break;
                    case STRING:
                        s += generateStringOutput(paramView, atom);
                        break;

                    default:
                        new WarningMessage.UnknownParamType(paramView).report();
                    case INT:
                        s += evaluate(atom);
                        break;
                }
                return s + '\n';
            }
            return "";
        }

        private Optional<int[]> getArrayContents(int arrayBaseAddress, ParamView arrayData) {
            return arrayData.getSize().map(size ->
                    Arrays.copyOfRange(memory, arrayBaseAddress, arrayBaseAddress + evaluate(size)));
        }

        private String generateArrayOutput(ParamView view, Atom arrayAddressAtom) {
            final int arrayBaseAddress = evaluate(arrayAddressAtom);
            return getArrayContents(arrayBaseAddress, view).map(Arrays::toString)
                    .orElse(String.valueOf(arrayBaseAddress)); // Only display address, if no contents can be fetched.
        }

        private String generateStringOutput(ParamView view, Atom arrayAddressAtom) {
            final int arrayBaseAddress = evaluate(arrayAddressAtom);
            final Optional<int[]> contents = getArrayContents(arrayBaseAddress, view);
            if (contents.isEmpty()) {
                return String.valueOf(arrayBaseAddress);
            }

            int[] asciiCodes = contents.get();
            char[] characters = new char[asciiCodes.length];

            for (int i = 0; i < asciiCodes.length; i++) {
                characters[i] = (char) asciiCodes[i];
            }

            return String.valueOf(characters);
        }

        private String generateStackOutput(ParamView view, Atom stackAddressAtom) {
            final int stackBaseAddress = evaluate(stackAddressAtom);
            final Optional<int[]> contents = getArrayContents(stackBaseAddress, view);

            if (contents.isEmpty()) {
                return String.valueOf(stackBaseAddress);
            }

            int[] stackElements = contents.get();
            if (stackElements.length == 0) return "nil";

            return Arrays.stream(stackElements).boxed()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "<", "]"));
        }
    }

}
