package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;

import java.util.stream.Stream;

/**
 * Will support stacks of plain resources, such as:
 *
 * <pre>
 *     test.json,localdev.json
 * </pre>
 *
 * Which means:
 *
 * <ul>
 *     <li>Load base config</li>
 *     <li>Override with {@code test.json}</li>
 *     <li>Override with {@code localdev.json}</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class SimpleConfigResolver<C extends Configuration> implements ConfigResolver<C> {

    private final String baseConfig;

    /**
     * @param baseConfig The base config
     */
    public SimpleConfigResolver(String baseConfig) {
        this.baseConfig = baseConfig;
    }

    /**
     * @param bootstrap Bootstrap
     * @return The base config we were constructed with
     */
    @Override
    public Stream<String> baseConfig(Bootstrap<C> bootstrap) {
        return Stream.of(baseConfig);
    }

    /**
     * The stacked element is the resource.
     * @param bootstrap Bootstrap
     * @param stack Stack element
     * @return The stack element
     */
    @Override
    public Stream<String> stackedConfig(Bootstrap<C> bootstrap, String stack) {
        return Stream.of(stack);
    }
}
