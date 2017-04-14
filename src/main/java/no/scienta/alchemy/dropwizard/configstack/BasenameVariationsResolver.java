package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
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
class BasenameVariationsResolver<C extends Configuration> implements ConfigResolver<C> {

    private final String[] commonConfigs;

    private final String baseConfig;

    /**
     * @param baseConfig Name of the base config
     */
    BasenameVariationsResolver(Class<C> baseConfig, String... commonConfigs) {
        this(Objects.requireNonNull(baseConfig, "baseConfig").getSimpleName(), commonConfigs);
    }

    private BasenameVariationsResolver(String baseConfig, String... commonConfigs) {
        this.baseConfig = Objects.requireNonNull(baseConfig, "baseConfig").trim();
        if (this.baseConfig.isEmpty()) {
            throw new IllegalStateException("baseConfig was empty string");
        }
        this.commonConfigs = commonConfigs.clone();
    }

    @Override
    public Stream<String> commonConfig() {
        return Arrays.stream(commonConfigs).flatMap(this::suffixed);
    }

    @Override
    public final Stream<String> baseConfig() {
        return variations("");
    }

    @Override
    public final Stream<String> stackedConfig(String stackedElement) {
        return variations(stackedElement);
    }

    private Stream<String> suffixed(String name) {
        return Arrays.stream(Suffix.values()).map(stacked(name, ""));
    }

    private Stream<String> variations(String stackedElement) {
        return Arrays.stream(Suffix.values()).map(stacked(baseConfig, stackedElement));
    }

    private Function<Suffix, String> stacked(String baseConfig, String name) {
        return suffix -> suffix.suffixed(baseConfig + variation(name));
    }

    private static String variation(String name) {
        return name == null || name.trim().isEmpty() ? "" : "-" + name;
    }
}
