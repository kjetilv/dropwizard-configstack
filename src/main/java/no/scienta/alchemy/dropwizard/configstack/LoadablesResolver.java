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

    /**
     * Find the selection of loadable resources suggested by the server command.
     *
     * @param serverCommand The argument passed to the {@link io.dropwizard.cli.ServerCommand server command}
     * @return Loadables
     */
    List<Loadable> resolveLoadables(String serverCommand) {
        List<String> stack = inputStack(serverCommand);
        List<Loadable> loadables = candidatePath(stack)
                .flatMap(this::loadable)
                .collect(Collectors.toList());

        failOnEmpty(stack, loadables);
        logProgress(stack, loadables);

        return loadables;
    }

    /**
     * @param input The argument to {@link io.dropwizard.cli.ServerCommand server} command
     * @return Stack as list
     */
    private List<String> inputStack(String input) {
        return Arrays.stream(input.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @param stack Input stack, see {@link #inputStack(String)}
     * @return All candidate configuration paths
     */
    private Stream<String> candidatePath(List<String> stack) {
        return Stream.of(
                resolver.commonConfig(),
                resolver.baseConfig(),
                stack.stream().flatMap(string ->
                        Stream.concat(
                                Arrays.stream(Suffix.values()).map(suff -> suff.suffixed(string)),
                                resolver.stackedConfig(string))
                )
        ).flatMap(Function.identity()).distinct();
    }

    /**
     * @param candidatePath Candidate path
     * @return Stream of loadable data from path, or empty stream if no data was found
     */
    private Stream<Loadable> loadable(String candidatePath) {
        try {
            return Stream.of(provider.open(candidatePath))
                    .filter(Objects::nonNull)
                    .map(Loadable.forPath(candidatePath));
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private void failOnEmpty(List<String> stack, List<Loadable> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            resolver.baseConfig().collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", stack) + "]" +
                            stack.stream()
                                    .flatMap(resolver::stackedConfig)
                                    .collect(Collectors.joining(", ")) +
                            ", paths: " + candidatePath(stack).collect(Collectors.joining(", ")));
        }
    }

    private void logProgress(List<String> stack, List<Loadable> loadables) {
        progressLogger.println(() -> getClass().getSimpleName() +
                ": Resolved config stack [" + String.join(", ", stack) + "] from paths:\n  " +
                loadables.stream().map(Loadable::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.resolver + " <= " + provider + "]";
    }
}
