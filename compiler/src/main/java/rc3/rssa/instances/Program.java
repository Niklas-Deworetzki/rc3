package rc3.rssa.instances;

import rc3.januscompiler.Compiler;
import rc3.lib.optimization.AvailableOptimization;
import rc3.lib.optimization.Optimization;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.blocks.ControlGraph;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * This class represents a whole, structured RSSA {@link Program}.
 * <p>
 * A {@link Program} has a {@link List} of global {@link Annotation Annotations}, that
 * are valid for the whole {@link Program} itself.
 * <p>
 * Additionally, a {@link Program} defines multiple procedures, each represented as a
 * {@link ControlGraph}, which is a graph out of {@link BasicBlock} with a unique entry
 * and exit point: The begin and end of a procedure.
 */
public final class Program {
    private final List<Annotation> globalAnnotations;
    private final Map<String, ControlGraph> procedures;

    public Program(List<Annotation> globalAnnotations, Map<String, ControlGraph> procedures) {
        this.globalAnnotations = globalAnnotations;
        this.procedures = procedures;
    }

    /**
     * Returns the {@link List} of global {@link Annotation Annotations}.
     */
    public List<Annotation> globalAnnotations() {
        return globalAnnotations;
    }

    /**
     * Returns a {@link Map} of every defined procedures and their name.
     */
    public Map<String, ControlGraph> procedures() {
        return procedures;
    }

    public ControlGraph getProcedure(String name) {
        return procedures.get(name);
    }

    public Set<String> procedureNames() {
        return procedures.keySet();
    }

    /**
     * The default entry point of a {@link Program}.
     */
    public static final String DEFAULT_MAIN_ENTRY = "main";

    /**
     * Returns an {@link Iterator} returning all entry points defined for this program.
     * <p>
     * Entry points can be defined using the <code>@Entry("procedure")</code> annotation,
     * where <i>procedure</i> is the name of the {@link Program}'s entry point.
     * <p>
     * A well-formed program should have at most one entry point defined. If no explicit
     * entry point is specified, the {@link Program#DEFAULT_MAIN_ENTRY default} should be used.
     *
     * @see Program#DEFAULT_MAIN_ENTRY
     */
    public static Iterator<String> entryPoints(List<Annotation> annotations) {
        return annotations.stream()
                .filter(Annotation.withIdentifier("Entry"))
                .map(annotation -> annotation.<String>get("value"))
                .iterator();
    }

    /**
     * Returns the entry point for this {@link Program}.
     * <p>
     * If an <code>@Entry</code> {@link Annotation} is specified as a
     * {@link Program#globalAnnotations() global Annotation} of this {@link Program},
     * its <var>value</var> field is returned.
     * <p>
     * If no such {@link Annotation} is present, {@link Program#DEFAULT_MAIN_ENTRY}
     * is returned as the default entry point.
     */
    public String getEntryPoint() {
        final var entryPoints = entryPoints(globalAnnotations);
        if (entryPoints.hasNext()) return entryPoints.next();
        else return DEFAULT_MAIN_ENTRY;
    }

    /**
     * Returns an {@link Annotation} with the given identifier, if present.
     */
    public Optional<Annotation> findAnnotation(String identifier) {
        return globalAnnotations.stream()
                .filter(Annotation.withIdentifier(identifier))
                .findAny();
    }


    /**
     * Applies the given {@link Consumer} to every {@link Instruction} within this {@link Program}.
     */
    public void forEachInstruction(Consumer<? super Instruction> action) {
        for (ControlGraph procedure : procedures.values()) {
            for (BasicBlock block : procedure.getNodes()) {
                for (Instruction instruction : block) {
                    action.accept(instruction);
                }
            }
        }
    }

    private List<Instruction> generatedListOfProcedures = null;

    public List<Instruction> asListOfInstructions() {
        if (generatedListOfProcedures == null) {
            generatedListOfProcedures = procedures.values().stream()
                    .flatMap(graph -> graph.toCode().stream())
                    .toList();
        }

        return generatedListOfProcedures;
    }


    public void applyOptimization(ProcedureOptimization procedureOptimization) {
        procedures.replaceAll(procedureOptimization);
    }

    public static abstract class ProcedureOptimization extends Optimization
            implements BiFunction<String, ControlGraph, ControlGraph> {

        public ProcedureOptimization(AvailableOptimization optimization, Compiler compiler) {
            super(optimization, compiler.getOptimizationState());
        }
    }
}
