package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.io.ByteStreams;
import io.dropwizard.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

final class Loadable {

    private final String path;

    private final byte[] contents;

    private final String streamString;

    static Function<InputStream, Loadable> forPath(String path) {
        return stream -> new Loadable(path, stream);
    }

    private Loadable(String path, InputStream stream) {
        this.path = Objects.requireNonNull(path, "path");
        try {
            this.contents = stream == null ? null : ByteStreams.toByteArray(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load from " + path, e);
        }
        this.streamString = stream == null ? null : stream.toString();
    }

    String getPath() {
        return path;
    }

    boolean isYaml() {
        return Suffix.YAML.isSuffixed(this.path);
    }

    boolean hasContent() {
        return contents != null;
    }

    InputStream getStream() {
        return new ByteArrayInputStream(contents);
    }

    private boolean startsWith(Class<? extends Configuration> configurationClass, String next) {
        return path.toLowerCase().startsWith(configurationClass.getName() + next);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + path + ": " + (
                contents == null ? "unresolved" : contents.length + " bytes <= " + streamString
        ) + "]";
    }
}
