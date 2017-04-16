package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LoadablesResolver<C extends Configuration> {

    private final ConfigResolver<C> resolver;

    private final ProgressLogger progressLogger;

    private final ConfigurationSourceProvider provider;

    /**
     * @param resolver       How to resolve base config and stacked elements
     * @param progressLogger How to log progress, may be null
     */
    LoadablesResolver(ConfigurationSourceProvider provider, ConfigResolver<C> resolver, ProgressLogger progressLogger) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
    }

    List<Loadable> resolveLoadables(String path) {
        String[] stack = stackedElements(path);
        List<Loadable> loadables = paths(stack)
                .flatMap(this::read)
                .collect(Collectors.toList());
        audit(stack, paths(stack), loadables);
        return loadables;
    }

    private String[] stackedElements(String input) {
        return Arrays.stream(input.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(s -> !s.trim().isEmpty())
                .toArray(String[]::new);
    }

    private Stream<String> paths(String[] stack) {
        return Stream.of(
                resolver.commonConfig(),
                resolver.baseConfig(),
                Arrays.stream(stack)
                        .flatMap(string ->
                                Stream.concat(
                                        Arrays.stream(Suffix.values()).map(suff -> suff.suffixed(string)),
                                        resolver.stackedConfig(string))
                        )
        ).flatMap(Function.identity()).distinct();
    }

    private Stream<Loadable> read(String path) {
        try {
            return Stream.of(provider.open(path)).filter(Objects::nonNull).map(Loadable.forPath(path));
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private void audit(String[] stack, Stream<String> paths, List<Loadable> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            resolver.baseConfig().collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", Arrays.asList(stack)) + "]" +
                            Arrays.stream(stack)
                                    .flatMap(resolver::stackedConfig)
                                    .collect(Collectors.joining(", ")) +
                            ", paths: " + paths.collect(Collectors.joining(", ")));
        }

        progressLogger.println(() -> getClass().getSimpleName() +
                ": Resolved config stack [" + String.join(", ", Arrays.asList(stack)) + "] from paths:\n  " +
                loadables.stream().map(Loadable::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.resolver + " <= " + provider + "]";
    }
}
