package rc3.januscompiler.backend.rssa.toC;

import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.instances.Atom;
import rc3.rssa.instances.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is used in the {@link CTranslationVisitor} as support for the translation of RSSA procedures
 * to C functions.
 * <p>
 * Since RSSA provides <i>pass by value</i> semantics for procedure calls and is able to return an arbitrary number
 * of values per invoked procedure, this behavior has to be implemented in C as well. In C however it is not as
 * easy to pass an arbitrary number of results as a result from a function as value.
 * <p>
 * It would be possible to allocate some sort of container on the heap, that holds the returned values. But this
 * approach results in a whole new set of problems:
 * <ul>
 *  <li>Memory management for this container would have to be implemented. The container needs to be allocated and
 *  freed after use, making procedure calls error prone, giving them the possibility to fail without obvious reasons
 *  for an outside observer/user and making them probably really slow.</li>
 *  <li>The container would have to be unpacked, since its values are used later on.</li>
 * </ul>
 * <p>
 * As seen above, the container approach does not seem feasible, which is why we chose another approach that uses
 * this class for implementation:
 * Ever translated C function is not only declared with the <i>explicit</i> parameter list as described by the
 * translated RSSA procedure, but also contains an <i>implicitly</i> declared parameter list.
 * This <i>implicit</i> parameter list holds a pointer for every value that is returned by the called procedure.
 * <p>
 * Using pointers to return the values of a called procedure allows us to pass an arbitrary amount of results,
 * since those pointers can easily be appended to the parameter list after listing all explicit parameters.
 * C also allows us to allocate memory on the stack for the returned values, since it is possible to declare
 * variables before calling a procedure and using pointers to those locally declared stack variables as arguments
 * for the implicitly added parameter list.
 * <p>
 * Since implicit parameters are present in every called and declared procedure and represent one self-contained
 * concept, this class is used as an layer of abstraction for the implicit parameter list.
 */
public final class ImplicitParameterList {
    private final List<String> inputParameterNames;
    private final List<String> outputArguments;

    private final int implicitParameters;

    private final CTranslationVisitor visitorInstance;

    public ImplicitParameterList(CTranslationVisitor visitorInstance,
                                 BasicBlock block, int implicitParameters) {
        this.visitorInstance = visitorInstance;
        this.implicitParameters = implicitParameters;
        this.inputParameterNames = generateInputParameterNames(block.entryPoint().getParameters());
        this.outputArguments = generateOutputArguments(block.exitPoint().getParameters());
    }

    // Since RSSA allows constants as formal parameters but C does not, we have to define temporary names for them.
    private List<String> generateInputParameterNames(List<Atom> formalParameters) {
        List<String> result = new ArrayList<>();

        for (Atom parameter : formalParameters) {
            String name = (parameter instanceof Constant) ?
                    visitorInstance.nextTemporaryVariable() : parameter.accept(visitorInstance);

            result.add(name);
        }
        return result;
    }

    private List<String> generateOutputArguments(List<Atom> formalArguments) {
        List<String> result = new ArrayList<>(formalArguments.size() + implicitParameters);
        for (Atom formalArgument : formalArguments) {
            result.add(formalArgument.accept(visitorInstance));
        }
        return result;
    }

    private <A> List<A> createImplicitList(Function<Integer, A> function) {
        List<A> result = new ArrayList<>(implicitParameters);
        for (int i = 0; i < implicitParameters; i++) {
            result.add(function.apply(i));
        }
        return result;
    }

    private static Function<String, String> asParameterDeclaration(boolean isPointer) {
        if (isPointer) return name -> "int * " + name;
        else return name -> "int " + name;
    }

    private static final Function<Integer, String> formatParameter =
            number -> String.format("_outparam_%d", number);


    /**
     * Returns the names of input parameters.
     * These are the names of formally defined parameters or temporary names for constants.
     */
    public List<String> getInputParameterNames() {
        return inputParameterNames;
    }

    /**
     * Returns declarations for input parameters.
     * These declarations are the names returned from {@link ImplicitParameterList#getInputParameterNames()}
     * modified to be declarations of <code>int</code> variables.
     */
    public List<String> getInputDeclarations() {
        return getInputParameterNames().stream()
                .map(asParameterDeclaration(false))
                .collect(Collectors.toList());
    }

    /**
     * Returns a {@link String} representation for every outgoing argument.
     */
    public List<String> getOutputArguments() {
        return outputArguments;
    }

    /**
     * Returns the names of implicit parameters that are appended to the explicit parameter lists.
     */
    public List<String> getImplicitParameterNames() {
        return createImplicitList(formatParameter);
    }

    /**
     * Returns declarations for implicit parameters.
     * These declarations are the names returned from {@link ImplicitParameterList#getImplicitParameterNames()}
     * modified to be declarations of <code>int *</code> variables.
     */
    public List<String> getImplicitDeclarations() {
        return createImplicitList(formatParameter.andThen(asParameterDeclaration(true)));
    }

    /**
     * Formats the complete input parameter list (without parentheses) inclusive implicit parameters.
     */
    public String formatInputParameters() {
        List<String> parameterList = new ArrayList<>(inputParameterNames.size() + implicitParameters);
        parameterList.addAll(getInputDeclarations());
        parameterList.addAll(getImplicitDeclarations());
        return String.join(", ", parameterList);
    }

    /**
     * Formats the complete output argument list (without parentheses) inclusive implicit arguments.
     */
    public String formatOutputArguments() {
        List<String> argumentList = new ArrayList<>(outputArguments.size() + implicitParameters);
        argumentList.addAll(getOutputArguments());
        argumentList.addAll(getImplicitParameterNames());
        return String.join(", ", argumentList);
    }
}
