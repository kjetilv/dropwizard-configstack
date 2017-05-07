package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bundles a {@link Bundle} that does configuration.
 *
 * @param <C> Configuration class
 */
@SuppressWarnings("unused")
public interface ConfigStackBundler<C extends Configuration> {

    /**
     * Returns a ready-to-go default bundle which respects classpath resources and substitutes variables from
     * system properties, environment and the config itself.
     *
     * @param configurationClass Configuration class
     * @param <C> Configuration type
     * @return A bundle with default settings
     */
    static <C extends Configuration> Bundle defaultBundle(Class<C> configurationClass) {
        return defaults(configurationClass).bundle();
    }

    /**
     * Returns a ready-to-go default bundler.  Will {@link #bundle() build} a default bundle which respects classpath
     * resources and substitutes variables from system properties, environment and the config itself.  The bundler
     * can be further customized before building.
     *
     * @param configurationClass Configuration class
     * @param <C> Configuration type
     * @return A bundler with default settings, for further bundling
     */
    static <C extends Configuration> ConfigStackBundler<C> defaults(Class<C> configurationClass) {
        return create(configurationClass)
                .enableClasspathResources()
                .enableVariableSubstitutions();
    }

    /**
     * Returns an "empty" bundler which loads from file and performs no substitution.  Intended for customization.
     *
     * @param configurationClass Configuration class
     * @param <C> Configuration type
     * @return A basic bundler
     */
    static <C extends Configuration> ConfigStackBundler<C> create(Class<C> configurationClass) {
        return new ConfigStackBundlerImpl<>(configurationClass);
    }

    /**
     * Set an alternative resolver.
     *
     * @param configurationResourceResolver Resolver
     * @return this bundler
     */
    ConfigStackBundler<C> setConfigurationResourceResolver(ConfigurationResourceResolver configurationResourceResolver);

    /**
     * Add base config to be loaded first.
     *
     * @param common Common configs
     * @return this bundler
     */
    ConfigStackBundler<C> addCommonConfig(String... common);

    /**
     * Enable classpath resources loading.  Inserts a provider which delegates to the existing provider, then
     * falls back to classpath.
     *
     * @return this bundler
     */
    ConfigStackBundler<C> enableClasspathResources();

    /**
     * Turn variable substitutions on.  Turns on use of a {@link ConfigurationSubstitutor} on the end result JSON.
     *
     * @return this bundler
     */
    ConfigStackBundler<C> enableVariableSubstitutions();

    /**
     * Override procedure for parsing server commands to stacks.
     *
     * @param configurationStacker Override configuration stacker
     * @return this bundler
     */
    ConfigStackBundler<C> setConfigurationStacker(ConfigurationStacker configurationStacker);

    /**
     * Override procedure for {@link LoadedData loading data} based on a
     * {@link io.dropwizard.cli.ServerCommand server command} argument.
     *
     * @param configurationLoader Override configuration loader
     * @return this bundler
     */
    ConfigStackBundler<C> setConfigurationLoader(ConfigurationLoader configurationLoader);

    /**
     * Override the procedure for building a config from {@link LoadedData loaded data}.
     *
     * @param configurationBuilder Override configuration builder
     * @return this bundler
     */
    ConfigStackBundler<C> setConfigurationBuilder(ConfigurationAssembler configurationBuilder);

    /**
     * Override the procedure for performing substitutions on a loaded configuration
     *
     * @param configurationSubstitutor Overrider configuration substitutor
     * @return this bundler
     */
    ConfigStackBundler<C> setConfigurationSubstitutor(ConfigurationSubstitutor configurationSubstitutor);

    /**
     * Set a different string replacer, which will be used by the default {@link ConfigurationSubstitutor}.
     *
     * @param substitutor Override string replacer
     * @return this bundler
     */
    ConfigStackBundler<C> setSubstitutor(StringSubstitutor substitutor);

    /**
     * Set a different array strategy, to be used by the default {@link ConfigurationAssembler}.  If not set,
     * {@link ArrayStrategy#OVERLAY} is used.
     *
     * @param arrayStrategy How to combine arrays
     * @return this bundler
     */
    ConfigStackBundler<C> setArrayStrategy(ArrayStrategy arrayStrategy);

    /**
     * Set a progress logger.
     *
     * @param progressLogger Progress logger
     * @return this bundler
     */
    ConfigStackBundler<C> setProgressLogger(Consumer<Supplier<String>> progressLogger);

    /**
     * How to log progress.
     *
     * @param progressLogger If null, quiet operation.
     * @return this bundler
     */
    ConfigStackBundler<C> setProgressLogger(ProgressLogger progressLogger);

    /**
     * Don't log progress.
     *
     * @return this bundler
     */
    default ConfigStackBundler<C> quiet() {
        return setProgressLogger(s -> {});
    }

    /**
     * @return The bundle
     */
    Bundle bundle();
}
