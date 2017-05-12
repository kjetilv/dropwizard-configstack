package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ConfigStackBundle implements Bundle, BundleFondle {

    private final Class<?> configurationClass;

    private final ConfigurationResourceResolver configurationResourceResolver;

    private final List<String> commonConfigs;

    private final ProgressLogger progressLogger;

    private final ArrayStrategy arrayStrategy;

    private final boolean classpathResources;

    private final boolean variableSubstitutions;

    private final ConfigurationStacker configurationStacker;

    private final ConfigurationLoader configurationLoader;

    private final ConfigurationAssembler configurationBuilder;

    private final ConfigurationSubstitutor configurationSubstitutor;

    private final StringSubstitutor substitutor;

    ConfigStackBundle(Class<?> configurationClass,
                      ConfigurationResourceResolver configurationResourceResolver,
                      List<String> commonConfigs,
                      ProgressLogger progressLogger,
                      ArrayStrategy arrayStrategy,
                      boolean classpathResources,
                      boolean variableSubstitutions,
                      ConfigurationStacker configurationStacker,
                      ConfigurationLoader configurationLoader,
                      ConfigurationAssembler configurationBuilder,
                      ConfigurationSubstitutor configurationSubstitutor,
                      StringSubstitutor substitutor) {
        this.configurationClass = Objects.requireNonNull(configurationClass, "configurationClass");
        this.configurationResourceResolver = configurationResourceResolver;
        this.commonConfigs = commonConfigs == null || commonConfigs.isEmpty()
                ? Collections.emptyList()
                : ImmutableList.copyOf(commonConfigs);
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
        this.classpathResources = classpathResources;
        this.variableSubstitutions = variableSubstitutions;
        this.configurationStacker = configurationStacker;
        this.configurationLoader = configurationLoader;
        this.configurationBuilder = configurationBuilder;
        this.configurationSubstitutor = configurationSubstitutor;
        this.substitutor = substitutor;
    }

    @Override
    public <C extends Configuration> C read(Class<C> configClass, String path, ObjectMapper objectMapper)
            throws IOException {
        ConfigurationResourceResolver configurationResourceResolver =
                getConfigurationResourceResolver();
        ConfigurationSourceProvider provider = buildProvider(
                configurationResourceResolver,
                null,
                objectMapper,
                Thread.currentThread().getContextClassLoader());
        InputStream data = provider.open(path);
        return objectMapper.readerFor(configClass).readValue(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        failOnMisconfiguration(bootstrap);

        ObjectMapper objectMapper = bootstrap.getObjectMapper();
        ConfigurationSourceProvider existingProvider = bootstrap.getConfigurationSourceProvider();
        ClassLoader classLoader = bootstrap.getClassLoader();

        ConfigurationResourceResolver configurationResourceResolver =
                getConfigurationResourceResolver();

        StackingConfigurationSourceProvider provider = buildProvider(
                configurationResourceResolver,
                existingProvider,
                objectMapper,
                classLoader);

        bootstrap.setConfigurationFactoryFactory(
                (klass, validator, om, propertyPrefix) ->
                        new EmptyInputOKYamlConfigurationFactory<>(
                                klass, validator, propertyPrefix,
                                configurationResourceResolver, provider, om));

        bootstrap.setConfigurationSourceProvider(provider);
    }

    private StackingConfigurationSourceProvider buildProvider(
            ConfigurationResourceResolver configurationResourceResolver,
            ConfigurationSourceProvider existingProvider,
            ObjectMapper objectMapper,
            ClassLoader classLoader) {

        ConfigurationSourceProvider delegate =
                existingProvider == null ? new FileConfigurationSourceProvider() : existingProvider;

        ConfigurationSourceProvider provider = classpathResources
                ? new ClasspathFallbackProvider(delegate, classLoader)
                : delegate;

        ConfigurationLoader configurationLoader = getConfigurationLoader(configurationResourceResolver, provider);

        return new StackingConfigurationSourceProvider(
                getConfigurationStacker(),
                configurationResourceResolver,
                configurationLoader,
                getConfigurationBuilder(objectMapper),
                getConfigurationSubstitutor(),
                objectMapper,
                progressLogger);
    }

    private void failOnMisconfiguration(Bootstrap<?> bootstrap) {
        if (bootstrap.getConfigurationSourceProvider() instanceof StackingConfigurationSourceProvider) {
            throw new IllegalStateException
                    ("Please set a different source provider: " + bootstrap.getConfigurationSourceProvider());
        }
    }

    private ConfigurationStacker getConfigurationStacker() {
        return configurationStacker != null ? configurationStacker
                : new DefaultConfigurationStacker();
    }

    private ConfigurationResourceResolver getConfigurationResourceResolver() {
        return configurationResourceResolver != null
                ? configurationResourceResolver
                : new BasenameVariationsResourceResolver(configurationClass);
    }

    private ConfigurationSubstitutor getConfigurationSubstitutor() {
        if (configurationSubstitutor != null) {
            return configurationSubstitutor;
        }
        if (variableSubstitutions) {
            return new DefaultConfigurationSubstitutor(substitutor);
        }
        return node -> node;
    }

    private ConfigurationAssembler getConfigurationBuilder(ObjectMapper objectMapper) {
        return configurationBuilder != null ? configurationBuilder
                : new DefaultConfigurationAssembler(objectMapper, arrayStrategy);
    }

    private ConfigurationLoader getConfigurationLoader(ConfigurationResourceResolver configurationResourceResolver,
                                                       ConfigurationSourceProvider configurationSourceProvider) {
        if (configurationLoader != null) {
            return configurationLoader;
        }
        return new DefaultConfigurationLoader(
                configurationSourceProvider,
                configurationResourceResolver,
                commonConfigs,
                progressLogger);
    }

    @Override
    public void run(Environment environment) {
    }
}
