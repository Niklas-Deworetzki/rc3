package rc3.rssa.blocks;

import rc3.lib.utils.CollectionUtils;
import rc3.lib.utils.GraphViz;
import rc3.lib.utils.StringUtils;
import rc3.rssa.instances.*;

import java.util.*;
import java.util.stream.Collectors;

public class BasicBlock implements GraphViz, Iterable<Instruction> {

    final int number;
    private final List<Instruction> instructions = new ArrayList<>();

    public BasicBlock(int number) {
        this.number = number;
    }

    public BasicBlock reversed() {
        BasicBlock result = new BasicBlock(number);
        for (Instruction instruction : CollectionUtils.reverse(this.instructions)) {
            result.addInstruction(instruction.reverse());
        }
        return result;
    }

    public BasicBlock copyWithNewBody(List<Instruction> instructions) {
        BasicBlock result = new BasicBlock(number);
        instructions.forEach(result::addInstruction);
        return result;
    }

    public ControlInstruction entryPoint() {
        return (ControlInstruction) instructions.get(0);
    }

    public ControlInstruction exitPoint() {
        return (ControlInstruction) instructions.get(instructions.size() - 1);
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public void setInstructions(Collection<Instruction> instructions) {
        this.instructions.clear();
        this.instructions.addAll(instructions);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Set<String> entryLabels() {
        ControlInstruction entryPoint = entryPoint();
        if (entryPoint instanceof BeginInstruction) {
            return Set.of();
        } else {
            return entryPoint.associatedLabels();
        }
    }

    public Set<String> exitLabels() {
        ControlInstruction exitPoint = exitPoint();
        if (exitPoint instanceof EndInstruction) {
            return Set.of();
        } else {
            return exitPoint.associatedLabels();
        }
    }

    public Set<Variable> variablesCreated() {
        return instructions.stream()
                .flatMap(instruction -> instruction.variablesCreated().stream())
                .collect(Collectors.toSet());
    }

    public Set<Variable> variablesDestroyed() {
        return instructions.stream()
                .flatMap(instruction -> instruction.variablesDestroyed().stream())
                .collect(Collectors.toSet());
    }

    public Set<Variable> variablesUsedOrDestroyed() {
        return instructions.stream()
                .flatMap(instruction -> instruction.variablesUsedOrDestroyed().stream())
                .collect(Collectors.toSet());
    }

    public List<Atom> inputParameters() {
        return entryPoint().getParameters();
    }

    public List<Atom> outputParameters() {
        return exitPoint().getParameters();
    }

    @Override
    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }

    @Override
    public String graphString() {
        String contents = instructions.stream()
                .map(String::valueOf).map(StringUtils::escape)
                .collect(Collectors.joining("\\l", "", "\\l"));

        return String.format("%s[label=\"Block #%d\\n%s\"]",
                graphId(),
                number,
                contents);
    }

    @Override
    public String graphId() {
        return String.format("B%s", String.join("", entryPoint().associatedLabels()));
    }
}
