package no.scienta.alchemy.dropwizard.configstack;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Accepts a base config and resolves stacked configs as:
 * <p>
 * <pre>
 *     [base config]-[stacked element name].[suffix]
 * </pre>
 * <p>
 * Will support stacks like:
 * <p>
 * <pre>
 *     test,localdev
 * </pre>
 * <p>
 * or
 * <p>
 * <pre>
 *     test;localdev
 * </pre>
 * <p>
 * Which means, if base config is e.g. {@code AppConfig} and {@link Suffix#JSON JSON} is suffix:
 * <p>
 * <ul>
 * <li>Load base config {@code AppConfig.json}</li>
 * <li>Override with {@code AppConfig-test.json}</li>
 * <li>Override with {@code AppConfig-localdev.json}</li>
 * </ul>
 */
final class BasenameVariationsResourceResolver implements ConfigurationResourceResolver {

    private final String baseConfig;

    /**
     * @param baseConfig Name of the base config
     */
    BasenameVariationsResourceResolver(Class<?> baseConfig) {
        this(Objects.requireNonNull(baseConfig, "baseConfig").getSimpleName());
    }

    private BasenameVariationsResourceResolver(String baseConfig) {
        this.baseConfig = Objects.requireNonNull(baseConfig, "baseConfig").trim();
        if (this.baseConfig.isEmpty()) {
            throw new IllegalStateException("baseConfig was empty string");
        }
    }

    @Override
    public final Stream<String> baseResource() {
        return Stream.of(baseConfig);
    }

    @Override
    public final Stream<String> stackedResource(String stackedElement) {
        return Stream.of(baseConfig + variation(stackedElement));
    }

    private static String variation(String name) {
        return name == null || name.trim().isEmpty() ? "" : "-" + name;
    }
}
