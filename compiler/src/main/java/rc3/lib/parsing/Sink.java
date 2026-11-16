package rc3.lib.parsing;

import java.io.*;

/**
 * This class represents an output, the application can write text to.
 * Currently {@link java.io.File Files} are supported as writable targets
 * as well as the {@link System#out default output stream}.
 * <p>
 * A {@link Sink} instance is acquired by calling {@link Sink#fromFileOrDefault(File)},
 * which will open a {@link PrintStream} to the given {@link File} (if present) or
 * provide a facility around {@link System#out} if <code>null</code> is passed
 * instead.
 */
public class Sink implements Closeable {
    private final File outputFile;
    private final PrintStream outputStream;

    protected Sink(File file) throws IOException {
        this.outputFile = file;
        this.outputStream = open();
    }

    /**
     * Create a new {@link Sink} for the given {@link File} if present.
     * If <code>null</code> is passed, a {@link Sink} writing to {@link System#out}
     * is created instead.
     */
    public static Sink fromFileOrDefault(File file) throws IOException {
        return new Sink(file);
    }

    public PrintStream getOutputStream() {
        return outputStream;
    }

    protected PrintStream open() throws IOException {
        if (outputFile == null) return System.out;
        else return new PrintStream(new FileOutputStream(outputFile), true);
    }

    @Override
    public void close() {
        if (outputFile != null && outputStream != null) {
            outputStream.close();
        }
    }
}
