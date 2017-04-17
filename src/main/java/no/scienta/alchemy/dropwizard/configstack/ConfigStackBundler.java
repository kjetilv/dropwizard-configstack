package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public final class ConfigStackBundler<C extends Configuration> {

    public static <C extends Configuration> ConfigStackBundler<C> defaults(Class<C> configurationClass) {
        return new ConfigStackBundler<>(configurationClass)
                .enableClasspathResources()
                .enableVariableSubstitutions();
    }

    private final Class<C> configurationClass;

    private ApplicationConfigurationResolver resolver;

    private final List<String> common = new ArrayList<>();

    private ArrayStrategy arrayStrategy = ArrayStrategy.OVERLAY;

    private ProgressLogger progressLogger = DEFAULT_PROGRESS_LOGGER;

    private boolean classpathResources;

    private boolean variableSubstitutions;

    private ConfigurationLoader configurationLoader;

    private ConfigurationCombiner configurationCombiner;

    private ConfigurationSubstitutor configurationSubstitutor;

    private StringSubstitutor substitutor;

    public ConfigStackBundler(Class<C> configurationClass) {
        this.configurationClass = Objects.requireNonNull(configurationClass, "configurationClass");
    }

    /**
     * Set an alternative resolver
     *
     * @param resolver Resolver
     * @return this bundler
     */
    public ConfigStackBundler<C> setResolver(ApplicationConfigurationResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        return this;
    }

    public ConfigStackBundler<C> addCommonConfig(String... common) {
        this.common.addAll(Arrays.asList(common));
        return this;
    }

    /**
     * Enable classpath resources loading
     *
     * @return this bundler
     */
    public ConfigStackBundler<C> enableClasspathResources() {
        this.classpathResources = true;
        return this;
    }

    /**
     * Turn variable substitutions on.
     *
     * @return this bundler
     */
    public ConfigStackBundler<C> enableVariableSubstitutions() {
        this.variableSubstitutions = true;
        return this;
    }

    public ConfigStackBundler<C> setConfigurationLoader(ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
        return this;
    }

    public ConfigStackBundler<C> setConfigurationCombiner(ConfigurationCombiner configurationCombiner) {
        this.configurationCombiner = configurationCombiner;
        return this;
    }

    public ConfigStackBundler<C> setConfigurationSubstitutor(ConfigurationSubstitutor configurationSubstitutor) {
        this.configurationSubstitutor = configurationSubstitutor;
        return this;
    }

    /**
     * Set a different replacer.
     *
     * @param substitutor Alternative replacer
     * @return this bundler
     */
    public ConfigStackBundler<C> setSubstitutor(Function<String, String> substitutor) {
        return setSubstitutor((StringSubstitutor) substitutor);
    }

    /**
     * Set a different replacer.
     *
     * @param substitutor Alternative replacer
     * @return this bundler
     */
    public ConfigStackBundler<C> setSubstitutor(StringSubstitutor substitutor) {
        Objects.requireNonNull(substitutor, "substitutor");
        enableVariableSubstitutions();
        this.substitutor = substitutor;
        return this;
    }

    /**
     * How to combine arrays.  If not set, {@link ArrayStrategy#OVERLAY} is used.
     *
     * @param arrayStrategy How to combine arrays
     * @return this bundler
     */
    public ConfigStackBundler<C> setArrayStrategy(ArrayStrategy arrayStrategy) {
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
        return this;
    }

    public ConfigStackBundler<C> setProgressLogger(Consumer<Supplier<String>> progressLogger) {
        return setProgressLogger((ProgressLogger) progressLogger);
    }

    /**
     * How to log progress.
     *
     * @param progressLogger If null, quiet operation.
     * @return this bundler
     */
    public ConfigStackBundler<C> setProgressLogger(ProgressLogger progressLogger) {
        this.progressLogger = Objects.requireNonNull(progressLogger);
        return this;
    }

    /**
     * Don't log progress.
     *
     * @return this bundler
     */
    public ConfigStackBundler<C> quiet() {
        return setProgressLogger(s -> {
        });
    }

    public ConfigStackBundle bundle() {
        ApplicationConfigurationResolver resolver = this.resolver == null
                ? new BasenameVariationsResolver(configurationClass)
                : this.resolver;
        progressLogger.println(() -> "Creating bundle for config " + configurationClass + "\n" +
                (common.isEmpty() ? "" : "  common: " + String.join(", ", common) + "\n") +
                ("  fall back to classpath: " + classpathResources + "\n") +
                ("  variable substitutions: " + variableSubstitutions + "\n"));
        return new ConfigStackBundle(
                resolver,
                common,
                progressLogger,
                arrayStrategy,
                classpathResources,
                variableSubstitutions,
                configurationLoader,
                configurationCombiner,
                configurationSubstitutor,
                substitutor);
    }

    static final ProgressLogger DEFAULT_PROGRESS_LOGGER = supplier -> System.out.println(supplier.get());
}
