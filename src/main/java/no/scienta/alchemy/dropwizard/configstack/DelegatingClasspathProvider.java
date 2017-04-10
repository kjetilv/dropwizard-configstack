package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adult {@link ConfigurationSourceProvider} which properly uses the {@link Thread#currentThread()} and its
 * {@link Thread#contextClassLoader context class loader} to resolve a classpath resources.
 */
class DelegatingClasspathProvider implements ConfigurationSourceProvider {

    private final Bootstrap<?> bootstrap;

    private final boolean fallbackIsFile;

    DelegatingClasspathProvider(Bootstrap<?> bootstrap) {
        this.bootstrap = bootstrap;
        this.fallbackIsFile =
                bootstrap.getConfigurationSourceProvider() instanceof FileConfigurationSourceProvider;
    }

    @Override
    public InputStream open(String base) throws IOException {
        for (String path : paths(base)) {
            if (loadableAsFile(path)) {
                InputStream config = openFileFallback(path);
                if (config != null) { // Found as file
                    return config;
                }
            }
            if (cl().getResource(path) != null) {
                InputStream config = cl().getResourceAsStream(path);
                if (config != null) { // Found on classpath
                    return config;
                }
            }
            InputStream config = openFileFallback(path); // Last-ditch
            if (config != null) { // Found... somehow!
                return config;
            }
        }
        return null; // No dice
    }

    private String[] paths(String base) {
        Class<?> configClass = bootstrap.getApplication().getConfigurationClass();
        return base.startsWith(configClass.getSimpleName()) && base.endsWith(JSON_SUFF)
                ? new String[]{base}
                : new String[]{base, configClass.getSimpleName() + "-" + base + JSON_SUFF};
    }

    private ClassLoader cl() {
        return bootstrap.getClassLoader() != null
                ? bootstrap.getClassLoader()
                : Thread.currentThread().getContextClassLoader();
    }

    private boolean loadableAsFile(String path) {
        return fallbackIsFile && new File(path).isFile() && new File(path).canRead();
    }

    private InputStream openFileFallback(String path) {
        try {
            return bootstrap.getConfigurationSourceProvider().open(path);
        } catch (IOException ignore) {
            // Not sure whether this should be logged
        }
        return null;
    }

    private static final String JSON_SUFF = ".json";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                bootstrap.getApplication().getConfigurationClass().getSimpleName() +
                (fallbackIsFile ? ", checking files first" : "") +
                "]";
    }
}
