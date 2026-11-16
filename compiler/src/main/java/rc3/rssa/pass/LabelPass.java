package rc3.rssa.pass;

import rc3.januscompiler.Direction;
import rc3.lib.DirectionVar;
import rc3.lib.messages.ErrorMessage;
import rc3.rssa.blocks.ControlGraph;
import rc3.rssa.blocks.ControlGraphGenerator;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.DoNothingVoidRSSAVisitor;

import java.util.*;

public final class LabelPass implements Pass.Transformation<Map<String, List<Instruction>>, Map<String, ControlGraph>> {

    @Override
    public Map<String, ControlGraph> execute(Map<String, List<Instruction>> input) throws ErrorMessage {
        final Collection<List<Instruction>> procedureImplementations = input.values();
        final LabelUsageVerifier verifier = new LabelUsageVerifier(getLabelDefinitions(procedureImplementations));

        for (List<Instruction> implementation : procedureImplementations) {
            for (Instruction instruction : implementation) {
                instruction.accept(verifier);
            }
        }

        return ControlGraphGenerator.fromProcedures(input);
    }

    private static class LabelUsageVerifier extends DoNothingVoidRSSAVisitor {
        private final DirectionVar<Map<String, ControlInstruction>> labelDefinitions;

        public LabelUsageVerifier(DirectionVar<Map<String, ControlInstruction>> labelDefinitions) {
            this.labelDefinitions = labelDefinitions;
        }

        private void checkIfLabelIsCorrect(String targetLabel, Direction sourceDirection,
                                           int parameterSize, Instruction instruction) {
            final Map<String, ControlInstruction> labels = labelDefinitions.get(sourceDirection.invert());
            if (!labels.containsKey(targetLabel)) {
                throw new ErrorMessage.Undefined("label", targetLabel,
                        instruction.getSourcePosition().orElse(null));
            }

            final int expectedArgumentCount = labels.get(targetLabel).getParameters().size();
            if (parameterSize != expectedArgumentCount) {
                throw new ErrorMessage.LabelParameterListSizeMismatch(targetLabel, expectedArgumentCount, parameterSize,
                        instruction.getSourcePosition().orElse(null));
            }
        }

        @Override
        protected void visitVoid(ConditionalEntry entry) {
            final int parameterSize = entry.getParameters().size();
            for (String label : entry.associatedLabels()) {
                checkIfLabelIsCorrect(label, Direction.FORWARD, parameterSize, entry);
            }
        }

        @Override
        protected void visitVoid(ConditionalExit exit) {
            final int parameterSize = exit.getParameters().size();
            for (String label : exit.associatedLabels()) {
                checkIfLabelIsCorrect(label, Direction.BACKWARD, parameterSize, exit);
            }
        }

        @Override
        protected void visitVoid(UnconditionalEntry entry) {
            final int parameterSize = entry.getParameters().size();
            for (String label : entry.associatedLabels()) {
                checkIfLabelIsCorrect(label, Direction.FORWARD, parameterSize, entry);
            }
        }

        @Override
        protected void visitVoid(UnconditionalExit exit) {
            final int parameterSize = exit.getParameters().size();
            for (String label : exit.associatedLabels()) {
                checkIfLabelIsCorrect(label, Direction.BACKWARD, parameterSize, exit);
            }
        }
    }

    private static DirectionVar<Map<String, ControlInstruction>> getLabelDefinitions(Collection<List<Instruction>> procedureImplementations) {
        final DirectionVar<Map<String, ControlInstruction>> labelDefinitions = DirectionVar.of(HashMap::new);

        for (List<Instruction> procedureImplementation : procedureImplementations) {
            for (Instruction instruction : procedureImplementation) {

                // ControlInstruction define labels
                if (instruction instanceof ControlInstruction control) {
                    Map<String, ControlInstruction> definitions = labelDefinitions.get(control.isEntryPoint ?
                            Direction.FORWARD : Direction.BACKWARD);

                    for (String associatedLabel : control.associatedLabels()) {
                        if (definitions.containsKey(associatedLabel)) {
                            throw new ErrorMessage.RedeclarationAs("label", associatedLabel,
                                    control.getSourcePosition().orElse(null));
                        }

                        definitions.put(associatedLabel, control);
                    }
                }
            }
        }

        return labelDefinitions;
    }
}
