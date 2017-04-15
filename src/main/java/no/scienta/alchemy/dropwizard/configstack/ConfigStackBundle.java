package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Objects;

class ConfigStackBundle<C extends Configuration> implements Bundle {

    private final ConfigResolver resolver;

    private final ProgressLogger progressLogger;

    private final ArrayStrategy arrayStrategy;

    private final boolean classpathResources;

    private final boolean substituteVariables;

    private final Substitutor substitutor;

    ConfigStackBundle(ConfigResolver resolver,
                      ProgressLogger progressLogger,
                      ArrayStrategy arrayStrategy,
                      boolean classpathResources,
                      boolean substituteVariables,
                      Substitutor substitutor) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.progressLogger = progressLogger;
        this.arrayStrategy = arrayStrategy;
        this.classpathResources = classpathResources;
        this.substituteVariables = substituteVariables;
        this.substitutor = substitutor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        if (classpathResources) {
            enableClasspathResources(bootstrap);
        }
        StackingConfigurationSourceProvider<C> provider = buildProvider(bootstrap);
        bootstrap.setConfigurationSourceProvider(provider);
    }

    private void enableClasspathResources(Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new DelegatingClasspathProvider(bootstrap));
    }

    @SuppressWarnings("unchecked")
    private StackingConfigurationSourceProvider<C> buildProvider(Bootstrap<?> bootstrap) {
        return new StackingConfigurationSourceProvider<>(
                bootstrap,
                resolver,
                arrayStrategy,
                substituteVariables,
                substitutor,
                progressLogger);
    }

    @Override
    public void run(Environment environment) {
    }
}
