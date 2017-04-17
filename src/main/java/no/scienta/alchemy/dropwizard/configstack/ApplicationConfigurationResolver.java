package no.scienta.alchemy.dropwizard.configstack;

import java.util.stream.Stream;

/**
 * Responsible for providing the lookup-able resources for the
 * {@link io.dropwizard.Configuration configuration class} of an application.
 */
public interface ApplicationConfigurationResolver {

    /**
     * @return Resource for base config.
     */
    Stream<String> baseConfig();

    /**
     * @param stack Stack element
     * @return Resource for a stackable resource that overrides the {@link #baseConfig()}.
     */
    Stream<String> stackedConfig(String stack);
}
