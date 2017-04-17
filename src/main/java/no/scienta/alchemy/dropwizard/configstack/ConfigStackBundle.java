package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.collect.ImmutableList;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ConfigStackBundle implements Bundle {

    private final ConfigurationResolver configResolver;

    private final List<String> common;

    private final ProgressLogger progressLogger;

    private final ArrayStrategy arrayStrategy;

    private final boolean classpathResources;

    private final boolean variableSubstitutions;

    private final ConfigurationLoader configurationLoader;

    private final ConfigurationCombiner configurationCombiner;

    private final ConfigurationSubstitutor configurationSubstitutor;

    private final StringSubstitutor substitutor;

    ConfigStackBundle(ConfigurationResolver configResolver,
                      List<String> common,
                      ProgressLogger progressLogger,
                      ArrayStrategy arrayStrategy,
                      boolean classpathResources,
                      boolean variableSubstitutions,
                      ConfigurationLoader configurationLoader,
                      ConfigurationCombiner configurationCombiner,
                      ConfigurationSubstitutor configurationSubstitutor, StringSubstitutor substitutor) {
        this.configResolver = Objects.requireNonNull(configResolver, "resolver");
        this.common = common == null || common.isEmpty() ? Collections.emptyList() : ImmutableList.copyOf(common);
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
        this.classpathResources = classpathResources;
        this.variableSubstitutions = variableSubstitutions;
        this.configurationLoader = configurationLoader;
        this.configurationCombiner = configurationCombiner;
        this.configurationSubstitutor = configurationSubstitutor;
        this.substitutor = substitutor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        if (bootstrap.getConfigurationSourceProvider() instanceof StackingConfigurationSourceProvider) {
            throw new IllegalStateException
                    ("Please set a different source provider: " + bootstrap.getConfigurationSourceProvider());
        }
        if (classpathResources) {
            enableClasspathResources(bootstrap);
        }
        bootstrap.setConfigurationSourceProvider(buildProvider(bootstrap));
    }

    private void enableClasspathResources(Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new DelegatingClasspathProvider(bootstrap));
    }

    @SuppressWarnings("unchecked")
    private StackingConfigurationSourceProvider buildProvider(Bootstrap<?> bootstrap) {
        return new StackingConfigurationSourceProvider(
                getConfigurationLoader(bootstrap),
                getConfigurationResolver(bootstrap),
                getConfigurationSubstitutor(),
                bootstrap.getObjectMapper(),
                progressLogger);
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

    private ConfigurationCombiner getConfigurationResolver(Bootstrap<?> bootstrap) {
        if (configurationCombiner != null) {
            return configurationCombiner;
        }
        return new DefaultConfigurationCombiner(
                bootstrap.getObjectMapper(),
                arrayStrategy);
    }

    private ConfigurationLoader getConfigurationLoader(Bootstrap<?> bootstrap) {
        if (configurationLoader != null) {
            return configurationLoader;
        }
        return new DefaultConfigurationLoader(
                bootstrap.getConfigurationSourceProvider(),
                configResolver,
                common,
                progressLogger);
    }

    @Override
    public void run(Environment environment) {
    }
}
