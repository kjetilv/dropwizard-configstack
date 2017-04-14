package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.Objects;
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
        this.baseConfig = Objects.requireNonNull(baseConfig, "baseConfig").trim();
        if (this.baseConfig.isEmpty()) {
            throw new IllegalArgumentException("Empty base config");
        }
    }

    /**
     * @return The base config we were constructed with
     */
    @Override
    public Stream<String> baseConfig() {
        return Stream.of(baseConfig);
    }

    /**
     * The stacked element is the resource.
     * @param stack Stack element
     * @return The stack element
     */
    @Override
    public Stream<String> stackedConfig(String stack) {
        return Stream.of(stack);
    }
}
