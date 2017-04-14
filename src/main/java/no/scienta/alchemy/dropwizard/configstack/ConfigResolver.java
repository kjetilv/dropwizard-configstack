package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.stream.Stream;

/**
 * Resolve stacked elements.
 */
public interface ConfigResolver<C extends Configuration> {

    default Stream<String> commonConfig() {
        return Stream.empty();
    }

    /**
     * @return Resource for base config
     */
    Stream<String> baseConfig();

    /**
     * @param stack     Stack element
     * @return What the resource looks like
     */
    Stream<String> stackedConfig(String stack);
}
