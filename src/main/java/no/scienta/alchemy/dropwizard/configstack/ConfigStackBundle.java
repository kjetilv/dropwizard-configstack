package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Objects;

class ConfigStackBundle<C extends Configuration> implements Bundle {

    private final ConfigResolver resolver;

    private final ProgressLogger progressLogger;

    private final JsonCombiner jsonCombiner;

    private final boolean classpathResources;

    private final boolean variableReplacements;

    private final JsonReplacer.Replacer replacer;

    ConfigStackBundle(Class<C> configurationClass,
                      boolean classpathResources,
                      boolean variableReplacements,
                      JsonReplacer.Replacer replacer,
                      ProgressLogger progressLogger,
                      JsonCombiner jsonCombiner,
                      String... commonConfigs) {
        this(new BasenameVariationsResolver<>(configurationClass, commonConfigs),
                progressLogger,
                jsonCombiner,
                classpathResources,
                variableReplacements,
                replacer);
    }

    ConfigStackBundle(ConfigResolver resolver,
                      ProgressLogger progressLogger,
                      JsonCombiner jsonCombiner,
                      boolean classpathResources,
                      boolean variableReplacements,
                      JsonReplacer.Replacer replacer) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.progressLogger = progressLogger;
        this.jsonCombiner = jsonCombiner;
        this.classpathResources = classpathResources;
        this.variableReplacements = variableReplacements;
        this.replacer = replacer;
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
                jsonCombiner,
                variableReplacements,
                replacer,
                progressLogger);
    }

    @Override
    public void run(Environment environment) {
    }
}
