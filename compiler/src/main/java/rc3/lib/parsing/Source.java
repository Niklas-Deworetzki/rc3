package rc3.lib.parsing;

import java.io.*;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * This class represents the source of an input. Currently supported inputs are a {@link File} or the
 * {@link System#in standard input stream}.
 * <p>
 * A {@link Source} instance is acquired by calling {@link Source#fromFileOrDefault(File)} passing a
 * {@link File} object as input or providing <code>null</code>.
 * </p>
 * <p>
 * Every {@link Source} has a name, that is accessible via {@link Source#getName()}. It is also possible
 * to request a line within the source. This however may fail, since the {@link File} originally used to
 * create the {@link Source} may have been altered at the moment of call, or there wasn't a {@link File}
 * provided as the {@link Source} can represent an input stream.
 * </p>
 */
public class Source implements Closeable {
    private final File inputFile;
    private final String name;
    private final InputStream inputStream;

    protected Source(File inputFile, String name) throws FileNotFoundException {
        this.inputFile = inputFile;
        this.name = name;
        this.inputStream = open();
    }

    /**
     * Create a new {@link Source} for the given {@link File} if present.
     * If <code>null</code> is passed, a {@link Source} reading from {@link System#in}
     * is created instead.
     */
    public static Source fromFileOrDefault(File file) throws FileNotFoundException {
        if (file != null) return new Source(file, file.getPath());
        else return new Source(null, "<stdin>");
    }

    /**
     * Returns the name of this {@link Source}.
     *
     * @return The name of this {@link Source}.
     */
    public String getName() {
        return name;
    }

    public String getFileName() {
        return inputFile.getName();
    }

    /**
     * Tries to return the textual line represented by the given line number in the input
     * described by this {@link Source}.
     * <p>
     * If the {@link File} used to create this source was altered, this method may fail returning an empty result.
     * It is also possible, that this {@link Source} describes an input stream, in which case the result will be
     * empty too.
     * </p>
     *
     * @param lineNumber The number of the line to be fetched from the {@link File} described by this {@link Source}.
     *                   Line numbers start counting from one.
     * @return The textual line if possible, empty otherwise.
     */
    public Optional<String> getLine(int lineNumber) {
        if (inputFile == null) {
            return Optional.empty();
        }

        try (var linesStream = Files.lines(inputFile.toPath())) {
            Iterator<String> lines = linesStream.iterator();

            while (lineNumber-- > 1) {
                lines.next();
            }
            return Optional.of(lines.next());
        } catch (IOException | NoSuchElementException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Returns an {@link InputStream} to read from this {@link Source}.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String toString() {
        return "Source{" + name + '}';
    }


    protected InputStream open() throws FileNotFoundException {
        if (inputFile == null) return System.in;
        else return new FileInputStream(inputFile);
    }

    @Override
    public void close() throws IOException {
        if (inputFile != null && inputStream != null) {
            inputStream.close();
        }
    }
}
