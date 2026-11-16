package rc3.januscompiler.backend.rssa.toC;

import rc3.rssa.blocks.BasicBlock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * This class represents a C function acquired by translating
 * a RSSA {@link BasicBlock}.
 * <p>
 * While the functions prototype is immediately available, this class
 * delays the translation of the function implementation.
 */
public record CFunction(String prototype,
                        Consumer<PrintWriter> implementationWriter) {

    /**
     * Returns the function prototype as string.
     */
    public String getPrototype() {
        return prototype;
    }

    /**
     * Generates the complete function implementation into a {@link String}.
     *
     * @implNote The implementation is generated with every call.
     */
    public String getImplementation() {
        StringWriter buffer = new StringWriter();
        writeImplementation(new PrintWriter(buffer));
        return buffer.toString();
    }

    /**
     * Writes the complete function implementation to the given {@link PrintWriter}.
     */
    public void writeImplementation(PrintWriter printWriter) {
        implementationWriter.accept(printWriter);
    }
}
