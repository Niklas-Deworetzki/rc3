package rc3.januscompiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This static utility class provides source code and versioning infos at runtime.
 * <p>
 * Within the .JAR file of this class, a few additional resources have been packed, holding information about
 * the Maven repository and Git status, for the source code used to build said file. To load this information,
 * the static {@link SourceConfig#initialize()} method has to be called. Otherwise no information will be available.
 * </p>
 */
public final class SourceConfig {
    private SourceConfig() {
    }

    private static final Properties GIT_PROPERTIES = new Properties();
    private static final Properties BUILD_PROPERTIES = new Properties();

    /**
     * Initializes build and versioning information.
     *
     * @throws IOException if an error occurs loading this information.
     */
    public static void initialize() throws IOException {
        try (InputStream resource = Compiler.class.getResourceAsStream("/git.properties")) {
            GIT_PROPERTIES.load(resource);
        }
        try (InputStream resource = Compiler.class.getResourceAsStream("/build/version.properties")) {
            BUILD_PROPERTIES.load(resource);
        }
    }

    /**
     * Returns version of the Maven project.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static String getVersion() {
        return BUILD_PROPERTIES.getProperty("version", "UNKNOWN VERSION");
    }

    /**
     * Returns the id of the commit, that has been used to build this executable.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static String getCommitId() {
        return GIT_PROPERTIES.getProperty("git.commit.id.abbrev", "unknown");
    }

    /**
     * Returns the name of the branch, that has been used to build this executable.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static String getBranch() {
        return GIT_PROPERTIES.getProperty("git.branch", "unknown");
    }

    /**
     * Returns the timestamp of the commit, that has been used to build this executable.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static String getTimestamp() {
        return GIT_PROPERTIES.getProperty("git.commit.time", "unknown");
    }

    /**
     * Returns <code>true</code> if the source code, from which this executable was build, contains uncommited
     * changes.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static boolean isDirty() {
        return Boolean.parseBoolean(
                GIT_PROPERTIES.getProperty("git.dirty"));
    }

    /**
     * Returns <code>true</code> if the branch, from which this executable was build, is behind its remote version.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static boolean isBehind() {
        return Integer.getInteger(GIT_PROPERTIES.getProperty("git.local.branch.behind"), 0) > 0;
    }

    /**
     * Formats the information provided by this class into a single {@link String} to be shown to a user.
     *
     * @apiNote This class has to be initialized before calling this method.
     */
    public static String getVersionString() {
        String version = SourceConfig.getVersion();

        if (SourceConfig.isDirty()) {
            version += " (dirty build)";
        } else {
            version += String.format(" (branch %s, commit %s, %s)",
                    SourceConfig.getBranch(), SourceConfig.getCommitId(), SourceConfig.getTimestamp());
        }

        if (SourceConfig.isBehind()) {
            version += " [UPDATES AVAILABLE]";
        }

        return "Version " + version;
    }
}
