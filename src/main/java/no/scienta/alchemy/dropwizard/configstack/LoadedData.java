package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A configuration resource that was found and loaded.
 */
@SuppressWarnings("WeakerAccess")
public final class LoadedData {

    private final String path;

    private final byte[] contents;

    /**
     * @param path A path
     * @return A function that creates a loadable from the stream found at the path
     */
    static Function<InputStream, LoadedData> forPath(String path) {
        return stream -> create(path, stream);
    }

    static LoadedData create(String path, InputStream stream) {
        return new LoadedData(path, stream);
    }

    /**
     * Loads and caches data from the stream, to be retrieved from {@link #getStream()} later.
     *
     * @param path The path
     * @param stream The stream, ready for reading
     */
    private LoadedData(String path, InputStream stream) {
        this.path = Objects.requireNonNull(path, "path");
        try {
            this.contents = stream == null ? null : ByteStreams.toByteArray(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load from " + path, e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * @return Original path
     */
    String getPath() {
        return path;
    }

    /**
     * @return True iff this is a {@link Suffix#YAML} resource
     */
    boolean isYaml() {
        return Suffix.YAML.isSuffixed(this.path);
    }

    /**
     * @return True iff content was found at the resource path
     */
    boolean hasContent() {
        return contents != null;
    }

    /**
     * @return A new input stream for the content found at the path
     */
    InputStream getStream() {
        return new ByteArrayInputStream(contents);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                path + ": " + (contents == null ? "unresolved" : contents.length + " bytes") +
                "]";
    }
}
