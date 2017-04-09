package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;

import java.util.stream.Stream;

/**
 * Resolve stacked elements.
 */
public interface ConfigResolver<C extends Configuration> {

    default Stream<String> commonConfig(Bootstrap<C> bootstrap) {
        return Stream.empty();
    }

    /**
     * @param bootstrap Bootstrap
     * @return Resource for base config
     */
    Stream<String> baseConfig(Bootstrap<C> bootstrap);

    /**
     * @param bootstrap Bootstrap
     * @param stack Stack element
     * @return What the resource looks like
     */
    Stream<String> stackedConfig(Bootstrap<C> bootstrap, String stack);
}
