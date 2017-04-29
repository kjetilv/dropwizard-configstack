package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ConfigStackBundlerImpl<C extends Configuration> implements ConfigStackBundler<C> {

    private final Class<C> configurationClass;

    private ConfigurationStacker configurationStacker;

    private ConfigurationResourceResolver configurationResourceResolver;

    private ConfigurationLoader configurationLoader;

    private ConfigurationBuilder configurationBuilder;

    private ConfigurationSubstitutor configurationSubstitutor;

    private final List<String> common = new ArrayList<>();

    private StringSubstitutor substitutor;

    private ArrayStrategy arrayStrategy = ArrayStrategy.OVERLAY;

    private ProgressLogger progressLogger = DEFAULT_PROGRESS_LOGGER;

    private boolean classpathResources;

    private boolean variableSubstitutions;

    ConfigStackBundlerImpl(Class<C> configurationClass) {
        this.configurationClass = Objects.requireNonNull(configurationClass, "configurationClass");
    }

    @Override
    public ConfigStackBundler<C> setConfigurationResourceResolver(ConfigurationResourceResolver configurationResourceResolver) {
        this.configurationResourceResolver = Objects.requireNonNull(configurationResourceResolver, "configurationResolver");
        return this;
    }

    @Override
    public ConfigStackBundler<C> addCommonConfig(String... common) {
        this.common.addAll(Arrays.asList(common));
        return this;
    }

    @Override
    public ConfigStackBundler<C> enableClasspathResources() {
        this.classpathResources = true;
        return this;
    }

    @Override
    public ConfigStackBundler<C> enableVariableSubstitutions() {
        this.variableSubstitutions = true;
        return this;
    }

    @Override
    public ConfigStackBundler<C> setConfigurationLoader(ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
        return this;
    }

    @Override
    public ConfigStackBundler<C> setConfigurationBuilder(ConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
        return this;
    }

    @Override
    public ConfigStackBundler<C> setConfigurationSubstitutor(ConfigurationSubstitutor configurationSubstitutor) {
        this.configurationSubstitutor = configurationSubstitutor;
        return this;
    }

    @Override
    public ConfigStackBundler<C> setSubstitutor(Function<String, String> substitutor) {
        return setSubstitutor((StringSubstitutor) substitutor::apply);
    }

    @Override
    public ConfigStackBundler<C> setSubstitutor(StringSubstitutor substitutor) {
        Objects.requireNonNull(substitutor, "substitutor");
        enableVariableSubstitutions();
        this.substitutor = substitutor;
        return this;
    }

    @Override
    public ConfigStackBundler<C> setArrayStrategy(ArrayStrategy arrayStrategy) {
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
        return this;
    }

    @Override
    public ConfigStackBundler<C> setProgressLogger(Consumer<Supplier<String>> progressLogger) {
        return setProgressLogger(progressLogger::accept);
    }

    @Override
    public ConfigStackBundler<C> setProgressLogger(ProgressLogger progressLogger) {
        this.progressLogger = Objects.requireNonNull(progressLogger);
        return this;
    }

    @Override
    public Bundle bundle() {
        progressLogger.println(() -> "Creating bundle for config " + configurationClass + "\n" +
                (common.isEmpty() ? "" : "  common: " + String.join(", ", common) + "\n") +
                ("  fall back to classpath: " + classpathResources + "\n") +
                ("  variable substitutions: " + variableSubstitutions + "\n"));
        return new ConfigStackBundle(
                configurationClass,
                configurationResourceResolver,
                common,
                progressLogger,
                arrayStrategy,
                classpathResources,
                variableSubstitutions,
                configurationStacker,
                configurationLoader,
                configurationBuilder,
                configurationSubstitutor,
                substitutor);
    }

    private static final ProgressLogger DEFAULT_PROGRESS_LOGGER = supplier -> System.out.println(supplier.get());
}
