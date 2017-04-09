package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adult {@link ConfigurationSourceProvider} which properly uses the {@link Thread#currentThread()} and its
 * {@link Thread#contextClassLoader context class loader} to resolve a classpath resources.
 */
class DelegatingClasspathProvider implements ConfigurationSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DelegatingClasspathProvider.class);

    private final ConfigurationSourceProvider fallback;

    private final boolean fallbackIsFile;

    private final Class<?> configurationClass;

    private final ClassLoader bootstrapClassLoader;

    DelegatingClasspathProvider(Bootstrap<?> bootstrap) {
        this.fallback = bootstrap.getConfigurationSourceProvider();
        this.fallbackIsFile = this.fallback instanceof FileConfigurationSourceProvider;
        this.configurationClass = bootstrap.getApplication().getConfigurationClass();
        this.bootstrapClassLoader = bootstrap.getClassLoader();
    }

    @Override
    public InputStream open(String base) throws IOException {
        for (String path : paths(base)) {
            if (filePresentAndLoadableByFallback(path)) {
                InputStream stream = openFallback(path);
                if (stream != null) {
                    return stream;
                }
            }
            if (cl().getResource(path) != null) {
                InputStream stream = cl().getResourceAsStream(path);
                if (stream != null) {
                    return stream;
                }
            }
            InputStream stream = openFallback(path);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    private ClassLoader cl() {
        if (this.bootstrapClassLoader != null) {
            return this.bootstrapClassLoader;
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private String[] paths(String base) {
        return base.startsWith(configurationClass.getSimpleName()) && base.endsWith(JSON_SUFF)
                ? new String[]{base}
                : new String[]{base, this.configurationClass.getSimpleName() + "-" + base + JSON_SUFF};
    }

    private boolean filePresentAndLoadableByFallback(String path) {
        File file = new File(path);
        return file.isFile() && file.canRead() && fallbackIsFile;
    }

    private InputStream openFallback(String path) {
        try {
            return fallback.open(path);
        } catch (IOException e) {
            log.debug("{} failed to open <{}>: {}", fallback, path, e.toString());
        }
        return null;
    }

    private static final String JSON_SUFF = ".json";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                configurationClass.getSimpleName() + (fallbackIsFile ? ", checking files first" : "") +
                "]";
    }
}
