package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * Adult {@link ConfigurationSourceProvider} which properly uses the {@link Thread#currentThread()} and its
 * {@link Thread#contextClassLoader context class loader} to resolve a classpath resources.
 */
final class ClasspathFallbackProvider implements ConfigurationSourceProvider {

    private final ConfigurationSourceProvider delegate;

    private final ClassLoader classLoader;

    ClasspathFallbackProvider(ConfigurationSourceProvider provider, ClassLoader classLoader) {
        this.delegate = provider;
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    @Override
    public InputStream open(String path) throws IOException {
        InputStream fromDelegate = delegate(path);
        return fromDelegate != null ? fromDelegate
                : classLoader.getResource(path) != null ? classLoader.getResourceAsStream(path)
                : null;
    }

    private InputStream delegate(String path) {
        try {
            return delegate.open(path);
        } catch (IOException ignore) {
            // Not sure whether this should be logged
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[=>" + delegate + "]";
    }
}
