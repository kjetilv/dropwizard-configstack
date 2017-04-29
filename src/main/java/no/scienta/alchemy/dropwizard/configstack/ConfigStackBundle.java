package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.collect.ImmutableList;
import io.dropwizard.Bundle;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ConfigStackBundle implements Bundle {

    private final Class<?> configurationClass;

    private final ConfigurationResourceResolver configurationResourceResolver;

    private final List<String> commonConfigs;

    private final ProgressLogger progressLogger;

    private final ArrayStrategy arrayStrategy;

    private final boolean classpathResources;

    private final boolean variableSubstitutions;

    private final ConfigurationStacker configurationStacker;

    private final ConfigurationLoader configurationLoader;

    private final ConfigurationBuilder configurationBuilder;

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
                      ConfigurationBuilder configurationBuilder,
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

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        failOnMisconfiguration(bootstrap);

        ConfigurationResourceResolver configurationResourceResolver = getConfigurationResourceResolver();
        ConfigurationLoader configurationLoader = getConfigurationLoader(
                configurationResourceResolver,
                delegateConfigurationSourceProvider(bootstrap));

        bootstrap.setConfigurationSourceProvider
                (new StackingConfigurationSourceProvider(
                        getConfigurationStacker(),
                        configurationResourceResolver,
                        configurationLoader,
                        getConfigurationBuilder(bootstrap),
                        getConfigurationSubstitutor(),
                        bootstrap.getObjectMapper(),
                        progressLogger
                ));
    }

    private void failOnMisconfiguration(Bootstrap<?> bootstrap) {
        if (bootstrap.getConfigurationSourceProvider() instanceof StackingConfigurationSourceProvider) {
            throw new IllegalStateException
                    ("Please set a different source provider: " + bootstrap.getConfigurationSourceProvider());
        }
    }

    private ConfigurationSourceProvider delegateConfigurationSourceProvider(Bootstrap<?> bootstrap) {
        if (classpathResources) {
            ClasspathFallbackProvider classpathFallbackProvider =
                    new ClasspathFallbackProvider(bootstrap);
            bootstrap.setConfigurationSourceProvider(classpathFallbackProvider);
            return classpathFallbackProvider;
        }
        return bootstrap.getConfigurationSourceProvider();
    }

    private ConfigurationStacker getConfigurationStacker() {
        return configurationStacker != null ? configurationStacker
                : new DefaultConfigurationStacker();
    }

    private ConfigurationResourceResolver getConfigurationResourceResolver() {
        return configurationResourceResolver != null ? configurationResourceResolver
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

    private ConfigurationBuilder getConfigurationBuilder(Bootstrap<?> bootstrap) {
        return configurationBuilder != null ? configurationBuilder
                : new DefaultConfigurationBuilder(bootstrap.getObjectMapper(), arrayStrategy);
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
