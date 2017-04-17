package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adult {@link ConfigurationSourceProvider} which properly uses the {@link Thread#currentThread()} and its
 * {@link Thread#contextClassLoader context class loader} to resolve a classpath resources.
 */
final class DelegatingClasspathProvider implements ConfigurationSourceProvider {

    private final Bootstrap<?> bootstrap;

    DelegatingClasspathProvider(Bootstrap<?> bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public InputStream open(String path) throws IOException {
        if (isAccessibleFile(path)) {
            InputStream config = delegate(path);
            if (config != null) {
                return config;
            }
        }
        if (cl().getResource(path) != null) {
            InputStream config = cl().getResourceAsStream(path);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    private ClassLoader cl() {
        return bootstrap.getClassLoader() != null
                ? bootstrap.getClassLoader()
                : Thread.currentThread().getContextClassLoader();
    }

    private boolean isAccessibleFile(String path) {
        return new File(path).isFile() && new File(path).canRead();
    }

    private InputStream delegate(String path) {
        try {
            return bootstrap.getConfigurationSourceProvider().open(path);
        } catch (IOException ignore) {
            // Not sure whether this should be logged
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[=>" + bootstrap.getConfigurationSourceProvider() + "]";
    }
}
