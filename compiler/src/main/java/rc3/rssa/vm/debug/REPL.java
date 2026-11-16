package rc3.rssa.vm.debug;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import rc3.januscompiler.Direction;
import rc3.rssa.vm.RSSARuntimeException;
import rc3.rssa.vm.RSSAVM;
import rc3.rssa.vm.StackFrame;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.jline.builtins.Completers.TreeCompleter;
import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * This class represents a debug read-evaluate-print-loop.
 * To execute and debug a RSSA program this class controls an instance of {@link RSSAVM}.
 */
public class REPL implements Runnable {

    private Terminal terminal;
    private final RSSAVM vm;
    private LineReader reader;

    private boolean exit = false;
    private boolean step_output = true;

    private static final String INITIAL_PROMPT = "(rdb) ";
    private static final String DEBUGGER_PROMPT = "| ";

    private static final Map<String, String> SHORTCUTS = Map.of(
            "bt", "backtrace",
            "b", "breakpoint",
            "c", "continue",
            "q", "quit",
            "p", "print",
            "r", "run",
            "s", "step",
            "rs", "reverse-step",
            "rc", "reverse-continue",
            "rr", "reverse-run"
    );

    // Commands
    private final Map<String, Consumer<String[]>> commands = Map.ofEntries(
            entry("step", this::step),
            entry("break", this::breakpoint),
            entry("backtrace", this::backtrace),
            entry("run", this::runProgram),
            entry("info", this::info),
            entry("continue", this::continueExec),
            entry("reverse", this::reverse),
            entry("reverse-step", this::reverseStep),
            entry("reverse-continue", this::reverseContinue),
            entry("reverse-run", this::reverseRun),
            entry("reverse-start", this::reverseStart),
            entry("start", this::start),
            entry("delete", this::delete),
            entry("help", this::help),
            entry("print", this::print),
            entry("set", this::set),
            entry("get", this::get),
            entry("quit", this::quit)
    );

    private final Completer completer = new TreeCompleter(
            node("info", node("program", "stack", "breakpoints", "direction", "next", "last", "count", "locals")),
            node("get", "set", node("print-main", "count-instructions", "step-output", node("true", "false"))),
            node(new StringsCompleter(commands.keySet())),
            node("print", node(new ActiveVariableCompleter())));

    protected class ActiveVariableCompleter implements Completer {
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            new StringsCompleter(getLocalVariableNames()).complete(reader, line, candidates);
        }
    }


    public REPL(RSSAVM vm) { // TODO: This could "throw IOException" removing the try-catch in constructor
        try {
            this.terminal = TerminalBuilder.builder().jansi(true).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.vm = vm;
        assert terminal != null; // This only happens if we catch the IOException above, does it?

        vm.setPrintStream(new PrintStream(terminal.output()));
        vm.enableDebugMode();
    }

    private LineReader openReader() {
        return LineReaderBuilder.builder()
                .appName("rdb")
                .completer(completer)
                .terminal(terminal)
                .build();
    }

    private String nativePrompt(LineReader reader) throws EndOfFileException {
        do {
            try {
                return promptInputOrDefault(reader::readLine);
            } catch (UserInterruptException e) {
                // Try again
            }
        } while (true);
    }

    private String previousInput = null;

    private String promptInputOrDefault(Function<String, String> generator) {
        String input;

        if (previousInput == null) {
            do {
                input = generator.apply(INITIAL_PROMPT);
            } while (input.isBlank());
        } else {
            input = generator.apply(INITIAL_PROMPT);
            if (input.isBlank()) {
                input = previousInput;
            }
        }

        previousInput = input;
        return input;
    }

    /**
     * This method executes the read-evaluate-print-loop while {@link #exit} is not true.
     */
    @Override
    public void run() {
        reader = openReader();
        println("Welcome to the RSSA VM Debugger: RDB!");
        while (!exit) {
            try {
                execute(nativePrompt(reader));
            } catch (EndOfFileException e) {
                quit(vm.getState().isRunning());
            }
            if (exit) {
                println("Goodbye!");
            }
        }
    }

    private void quit(boolean ask) {
        if (ask) {
            try {
                exit = ask("Do you want to quit the debugger? [y/N]\t", false);
            } catch (EndOfFileException e) {
                eofAssumption("y");
                exit = true;
            }
        } else {
            exit = true;
        }
    }

    /**
     * This executes a command by picking the right command function.
     *
     * @param input the read line representing a command
     */
    private void execute(String input) {
        input = input.trim();
        String[] line = input.split("\\s+");

        if (SHORTCUTS.containsKey(line[0])) {
            // Resolve shortcut if known.
            line[0] = SHORTCUTS.get(line[0]);
        }

        try {
            commands.getOrDefault(line[0], this::unknown).accept(line);
        } catch (RSSARuntimeException e) {
            // write rssa error onto the terminal, don't let it crush the debugger
            e.printStackTrace(terminal.writer());
            terminal.flush();
        }
    }

    ////////////////////////////////////
    //    COMMAND FUNCTIONS
    ////////////////////////////////////

    private void quit(String[] line) {
        assert line[0].equals("quit");
        if (line.length == 1) {
            quit(vm.getState().isRunning());
        } else {
            println("Usage: quit");
        }
    }

    private void step(String[] line) {
        ifVMIsRunning(() -> {
            int times = 1;
            if (line.length == 2) {
                try {
                    times = Integer.parseInt(line[1]);
                } catch (NumberFormatException e) {
                    println("Usage: " + (line[0].charAt(0) == 'r' ? "reverse-" : "") + "step [count]");
                    return; // or execute one step?
                }
                if (times <= 0) {
                    println("Step count less or equals 0");
                }
            }
            while (times-- > 0 && vm.getState() != RSSAVM.State.FINISHED) {
                vm.step(step_output);
            }
        });
    }

    private void breakpoint(String[] line) {
        assert line[0].equals("break") || line[0].equals("breakpoint");
        if (line.length == 2) {
            String argument = line[1];
            try {
                setBreakpoint(Integer.parseInt(argument), true);
            } catch (ArrayIndexOutOfBoundsException a) {
                printf("Index %s is out of bounds%n", argument);
            } catch (NumberFormatException e) {
                try {
                    setBreakpointsAtLabel(argument, true);
                } catch (RSSARuntimeException r) {
                    printf("Label '%s' not found%n", argument);
                }
            }
        } else {
            println("Usage: break[point] <ip> | break[point] <label>");
        }
    }

    private void runProgram(String[] line) {
        //assert line[0].equals("run");
        if (line.length == 1) {
            if (vm.getState() != RSSAVM.State.STOPPED
                    || ask("Do you want to restart the program? [Y/n]\t", true)) {
                vm.run();
            }
        } else {
            println("Usage: run");
        }
    }

    private void backtrace(String[] line) {
        assert line[0].equals("backtrace") || line[0].equals("bt");
        printCallStack();
    }

    private void printCallStack() {
        int idx = vm.getCallStack().size();
        if (idx == 0) {
            println("No stack.");
        } else {
            for (StackFrame stackFrame : vm.getCallStack()) {
                printf("#%03d\t%s%n", --idx, stackFrame);
            }
        }
    }

    private List<String> getLocalVariableNames() {
        Set<String> names = currentStackFrame()
                .map(StackFrame::getVariableNames)
                .map((x) -> Arrays.stream(x)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        List<String> sortedNames = new ArrayList<>(names);
        Collections.sort(sortedNames);
        return sortedNames;
    }

    private void info(String[] line) {
        assert line[0].equals("info");
        String helpText = "Usage: info program | info stack | info break[points] | info dir[ection] | info next " +
                "| info last | info count | info locals";
        if (line.length > 1) {
            switch (line[1]) {
                case "program":
                    println(vm.getState());
                    break;
                case "stack":
                    printCallStack();
                    break;
                case "break":
                case "breakpoints":
                    boolean hasBps = false;
                    for (int i = 0; i < vm.getBreakpoints().length; i++) {
                        if (vm.getBreakpoints()[i]) {
                            printf("%s\t#%03d\t%s%n", "Breakpoint at", i, vm.getInstruction(i));
                            hasBps = true;
                        }
                    }
                    if (!hasBps) {
                        println("No breakpoints.");
                    }
                    break;
                case "direction":
                case "dir":
                    printf("Execution direction is %s%n", vm.getDirection());
                    break;
                case "next":
                    ifVMIsRunning(() ->
                            printf("%s%n", vm.formatNextInstruction()));
                    break;
                case "last":
                    String instr = vm.getPreviousInstructionString();
                    ifVMIsRunning(() ->
                            printf("%s%n", instr == null ? "None." : instr));
                    break;
                case "count":
                    ifVMIsRunning(() -> printf(vm.instructionsCountOutput()));
                    break;
                case "locals":
                    currentStackFrame().ifPresentOrElse(stackFrame -> {
                        var names = getLocalVariableNames();
                        if (names.size() == 0) {
                            println("No locals.");
                        }
                        for (String name : names) {
                            try {
                                printf("%s = %d%n", name, stackFrame.get(name));
                            } catch (IllegalAccessException ignored) {

                            }

                        }
                    }, () -> println("No frame selected."));
                    break;
                default:
                    println(helpText);
            }
        } else {
            println(helpText);
        }
    }

    private void continueExec(String[] line) {
        assert line[0].equals("continue");
        println("Continuing.");
        if (line.length == 1) {
            ifVMIsRunning(() -> {
                vm.step(false); // execute paused instruction manually (to not pause again here)
                if (vm.getState().isRunning()) {
                    vm.execute();
                }
            });
        } else {
            println("Usage: " + (line[0].charAt(0) == 'r' ? "reverse-" : "") + "continue");
        }
    }

    private void reverse(String[] line) {
        assert line[0].equals("reverse");
        if (line.length == 1) {
            if (reverseIfAllowed()) {
                printf("Reversed execution direction to %s%n", vm.getDirection());
            }
        } else {
            println("Usage: reverse");
        }
    }

    private void start(String[] line) {
        //assert line[0].equals("start");
        if (line.length == 1) {
            if (vm.getState() != RSSAVM.State.STOPPED
                    || ask("Do you want to restart the program? [Y/n]\t", true)) {
                var entry = vm.getLabelAddr(vm.getMainEntry());
                if (!vm.getBreakpoints()[entry]) {
                    vm.setBreakpoint(entry, true);
                    vm.run();
                    vm.setBreakpoint(entry, false);
                } else {
                    vm.run();
                }
            }
        } else {
            println("Usage: start");
        }
    }

    private void delete(String[] line) {
        assert line[0].equals("delete");
        if (line.length == 2) {
            try {
                setBreakpoint(Integer.parseInt(line[1]), false);
            } catch (ArrayIndexOutOfBoundsException a) {
                printf("Index (%s) is out of bounds%n", line[1]);
            } catch (NumberFormatException e) {
                try {
                    setBreakpointsAtLabel(line[1], false);
                } catch (RSSARuntimeException r) {
                    printf("Label '%s' not found%n", line[1]);
                }
            }
        } else if (line.length == 1) {
            if (ask("Do you want to delete all breakpoints? [y/N]\t", false)) {
                for (int i = 0; i < vm.getBreakpoints().length; i++) {
                    if (vm.getBreakpoints()[i]) {
                        vm.setBreakpoint(i, false);
                    }
                }
                println("Deleted all breakpoints.");
            }
        } else {
            println("Usage: delete <ip> | delete <label>");
        }

    }

    private void print(String[] line) {
        assert line[0].equals("print");
        ifVMIsRunning(() -> {
            if (line.length == 2) {
                try {
                    var stackFrame = currentStackFrame();
                    if (stackFrame.isPresent()) {
                        printf("%s = %d%n", line[1], stackFrame.get().get(line[1]));
                    } else {
                        println("Stack is empty.");
                    }
                } catch (IllegalAccessException e) {
                    println(e.getMessage());
                }
            } else {
                println("Usage: print <variable>");
            }
        });
    }

    private void set(String[] line) {
        assert line[0].equals("set");
        String helpText = "Usage: set print-main <bool> | set count-instructions <bool> | set step-output <bool>";
        if (line.length >= 2) {
            switch (line[1]) {
                case "print-main":
                    if (line.length == 3) {
                        boolean value = Boolean.parseBoolean(line[2]);
                        vm.setShouldPrintMain(value);
                        get(new String[]{"get", line[1]});
                    } else {
                        println(helpText);
                    }
                    break;
                case "count-instructions":
                    if (line.length == 3) {
                        boolean value = Boolean.parseBoolean(line[2]);
                        vm.setShouldCountInstructions(value);
                        get(new String[]{"get", line[1]});
                    } else {
                        println(helpText);
                    }
                    break;
                case "step-output":
                    if (line.length == 3) {
                        step_output = Boolean.parseBoolean(line[2]);
                        get(new String[]{"get", line[1]});
                    } else {
                        println(helpText);
                    }
                    break;
                default:
                    println(helpText);
                    break;
            }
        } else {
            println(helpText);
        }
    }

    private void get(String[] line) {
        assert line[0].equals("get");
        String helpText = "Usage: get print-main | get count-instructions | get step-output";
        if (line.length == 2) {
            switch (line[1]) {
                case "print-main":
                    printf("Environment variable 'print-main' is %b%n", vm.getShouldPrintMain());
                    break;
                case "count-instructions":
                    printf("Environment variable 'count-instructions' is %b%n",
                            vm.getShouldCountInstructions());
                    break;
                case "step-output":
                    printf("Environment variable 'step-output' is %b%n", step_output);
                    break;
                default:
                    println(helpText);
                    break;
            }
        } else {
            println(helpText);
        }
    }

    private void reverseStep(String[] line) {
        ifVMIsRunning(() -> {
            if (reverseIfAllowed()) {
                step(line);
            }
        });
    }

    private void reverseContinue(String[] line) {
        ifVMIsRunning(() -> {
            if (reverseIfAllowed()) {
                continueExec(line);
            }
        });
    }

    private void reverseRun(String[] line) {
        if (line.length == 1) {
            if (vm.getState() != RSSAVM.State.STOPPED
                    || ask("Do you want to restart the program? [Y/n]\t", true)) {
                vm.reverseDirection();
                vm.run();
            }
        } else {
            println("Usage: reverse-run");
        }
    }

    private void reverseStart(String[] line) {
        if (line.length == 1) {
            if (vm.getState() != RSSAVM.State.STOPPED
                    || ask("Do you want to restart the program? [Y/n]\t", true)) {
                //setBreakpointsAtLabel(vm.getMainEntry(), true);
                vm.reverseDirection();
                var entry = vm.getLabelAddr(vm.getMainEntry());
                if (!vm.getBreakpoints()[entry]) {
                    vm.setBreakpoint(entry, true);
                    vm.run();
                    vm.setBreakpoint(entry, false);
                } else {
                    vm.run();
                }
            }
        } else {
            println("Usage: " + (line[0].charAt(0) == 'r' ? "reverse-" : "") + "start");
        }
    }

    private void unknown(String[] line) {
        printf("Unknown command '%s', type 'help' for further information%n", String.join(" ", line));
    }

    private void help(String[] line) {
        Map<String, String> help = Map.ofEntries(
                entry("run", "Sets up and runs the program up to a breakpoint"),
                entry("reverse-run", "Reverse direction and run"),
                entry("start", String.format("%s%n%-15s\t%s", "Enables temporary breakpoint at the current main begin instructions,",
                        DEBUGGER_PROMPT, "sets up and runs the program up to the breakpoint")),
                entry("reverse-start", "Reverse direction and start"),
                entry("step", "Executes n next instruction of the program"),
                entry("reverse-step", "Reverse direction temporary and step"),
                entry("continue", "Continues to run the stopped program"),
                entry("reverse-continue", "Reverse direction temporary and continue"),
                entry("break", "Enables a breakpoint at an ip or the instructions of a label"),
                entry("delete", "Deletes all breakpoints or a specific one"),
                entry("info", "Shows information about the program state and more"),
                entry("reverse", "Reverses the execution direction"),
                entry("set", "Set the value of a environment variable"),
                entry("get", "Get the value of a environment variable"),
                entry("print", "Prints the value of a variable on the current stack frame"),
                entry("backtrace", "Prints a backtrace of all stack frames."),
                entry("exit", "Exit this debugger"),
                entry("help", "Shows this help message")
        );

        println("Commands:");
        help.forEach((key, value) -> printf("%-15s\t%-20s%n", key, value));
    }

    ////////////////////////////////////
    //    HELPERS
    ////////////////////////////////////


    private void setBreakpoint(int idx, boolean value) {
        vm.setBreakpoint(idx, value);
        printf("%s breakpoint at #%03d\t%s%n", value ? "Enabled" : "Disabled",
                idx, vm.getInstruction(idx));
    }

    private void setBreakpointsAtLabel(String label, boolean value) {
        setBreakpoint(vm.getLabelAddr(label, Direction.FORWARD), value);
        setBreakpoint(vm.getLabelAddr(label, Direction.BACKWARD), value);
    }

    private void ifVMIsRunning(Runnable action) {
        if (vm.getState().isRunning()) {
            action.run();
        } else {
            println("Program has not been setup yet.");
        }
    }

    private void println(Object s) {
        terminal.writer().println(DEBUGGER_PROMPT + s);
        terminal.flush();
    }

    private void printf(String fmt, Object... args) {
        terminal.writer().printf(DEBUGGER_PROMPT + fmt, args);
        terminal.flush();
    }


    private boolean ask(String question, boolean defaultIsYes) {
        try {
            return confirm(reader.readLine(DEBUGGER_PROMPT + question), defaultIsYes);
        } catch (UserInterruptException e) {
            nativePrompt(reader);
        }
        return false;
    }

    private boolean confirm(String result, boolean defaultIsYes) {
        boolean yes = (result.isBlank() && defaultIsYes) || result.equalsIgnoreCase("y")
                || result.equalsIgnoreCase("yes");
        if (result.isBlank() && yes) {
            println("Confirmed.");
        } else if (result.isBlank()) {
            println("Not confirmed.");
        }
        return yes;
    }

    private void eofAssumption(String assumption) {
        printf("EOF [assumed %s]%n", assumption);
    }

    private Optional<StackFrame> currentStackFrame() {
        return Optional.ofNullable(vm.currentStackFrame());
    }

    private boolean reverseIfAllowed() {
        if (vm.getState().isRunning() && vm.getReverseIsForbidden()) {
            println("Reversing is forbidden, step instead and reverse then.");
            return false;
        }
        if (currentStackFrame().isPresent()
                && vm.getPreviousInstructionString() != null
                && vm.getPreviousInstructionString().contains("end")) {
            vm.setDirection(currentStackFrame().get().calleeDirection);

        } else if (currentStackFrame().isPresent()
                && vm.getPreviousInstructionString() != null
                && vm.getPreviousInstructionString().contains("call")
                && vm.getParseReturnArgs()) {

            vm.setDirection(currentStackFrame().get().callerDirection);
        }
        vm.swapIp();

        vm.reverseDirection();


        vm.getCallStack().forEach(frame -> {
            frame.callerDirection = frame.callerDirection.invert();
            frame.calleeDirection = frame.calleeDirection.invert();
        });
        return true;
    }

}

