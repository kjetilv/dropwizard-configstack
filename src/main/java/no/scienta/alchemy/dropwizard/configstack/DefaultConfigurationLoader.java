package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.collect.ImmutableList;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads data with a delegate {@link ConfigurationSourceProvider provider}
 */
final class DefaultConfigurationLoader implements ConfigurationLoader {

    private final ConfigurationResourceResolver configurationResourceResolver;

    private final List<String> commonConfigs;

    private final ProgressLogger progressLogger;

    private final ConfigurationSourceProvider delegateProvider;

    /**
     * @param configurationResourceResolver How to resolve base config and stacked elements
     * @param commonConfigs                 Common resources to be loaded across apps
     * @param progressLogger                How to log progress, may be null
     */
    DefaultConfigurationLoader(ConfigurationSourceProvider delegateProvider,
                               ConfigurationResourceResolver configurationResourceResolver,
                               List<String> commonConfigs,
                               ProgressLogger progressLogger) {
        this.delegateProvider = Objects.requireNonNull(delegateProvider, "provider");

        this.configurationResourceResolver = Objects.requireNonNull(configurationResourceResolver, "configResolver");
        this.commonConfigs = commonConfigs == null || commonConfigs.isEmpty()
                ? Collections.emptyList()
                : ImmutableList.copyOf(commonConfigs);
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
    }

    @Override
    public Collection<LoadedData> load(String serverCommand) {
        Collection<String> stack = inputStack(serverCommand);
        Collection<String> candidatePaths = candidatePaths(stack);
        Collection<LoadedData> loadables = candidatePaths.stream()
                .flatMap(this::loaded)
                .collect(Collectors.toList());

        failOnEmpty(stack, loadables);
        logProgress(stack, loadables);

        return loadables;
    }

    /**
     * @param input The argument to {@link io.dropwizard.cli.ServerCommand server} command
     * @return Stack as list
     */
    private Collection<String> inputStack(String input) {
        return Arrays.stream(input.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @param stack Input stack, see {@link #inputStack(String)}
     * @return All candidate configuration paths
     */
    private Collection<String> candidatePaths(Collection<String> stack) {
        return Stream.of(
                suffixed(commonConfigs.stream()),
                suffixed(configurationResourceResolver.baseResource()),
                stack.stream().flatMap(string ->
                        Stream.concat(
                                suffixedCandidates(string),
                                suffixed(configurationResourceResolver.stackedResource(string))))
        ).flatMap(Function.identity()).distinct().collect(Collectors.toList());
    }

    private Stream<String> suffixed(Stream<String> stream) {
        return stream.flatMap(DefaultConfigurationLoader::suffixedCandidates);
    }

    /**
     * @param candidatePath Candidate path
     * @return Stream of loadable data from path, or empty stream if no data was found
     */
    private Stream<LoadedData> loaded(String candidatePath) {
        try {
            return Stream.of(delegateProvider.open(candidatePath))
                    .filter(Objects::nonNull)
                    .map(LoadedData.forPath(candidatePath));
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private void failOnEmpty(Collection<String> stack, Collection<LoadedData> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            configurationResourceResolver.baseResource().collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", stack) + "]" +
                            stack.stream()
                                    .flatMap(configurationResourceResolver::stackedResource)
                                    .collect(Collectors.joining(", ")) +
                            ", paths: " + String.join(", ", candidatePaths(stack)));
        }
    }

    private void logProgress(Collection<String> stack, Collection<LoadedData> loadables) {
        progressLogger.println(() -> getClass().getSimpleName() +
                ": Resolved config stack [" + String.join(", ", stack) + "] from paths:\n  " +
                loadables.stream().map(LoadedData::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    private static Stream<String> suffixedCandidates(String name) {
        return Arrays.stream(Suffix.values()).map(suffix -> suffix.suffixed(name)).distinct();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.configurationResourceResolver + " <= " + delegateProvider + "]";
    }
}
