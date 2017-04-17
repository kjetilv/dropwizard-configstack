package no.scienta.alchemy.dropwizard.configstack;

import com.google.common.collect.ImmutableList;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads data with a delegate {@link ConfigurationSourceProvider provider}
 */
final class DefaultConfigurationLoader implements ConfigurationLoader {

    private final ApplicationConfigurationResolver configResolver;

    private final List<String> commonConfigs;

    private final ProgressLogger progressLogger;

    private final ConfigurationSourceProvider delegateProvider;

    /**
     * @param configResolver How to resolve base config and stacked elements
     * @param commonConfigs  Common resources to be loaded across apps
     * @param progressLogger How to log progress, may be null
     */
    DefaultConfigurationLoader(ConfigurationSourceProvider delegateProvider,
                               ApplicationConfigurationResolver configResolver,
                               List<String> commonConfigs,
                               ProgressLogger progressLogger) {
        this.delegateProvider = Objects.requireNonNull(delegateProvider, "provider");

        this.configResolver = Objects.requireNonNull(configResolver, "configResolver");
        this.commonConfigs = commonConfigs == null || commonConfigs.isEmpty()
                ? Collections.emptyList()
                : ImmutableList.copyOf(commonConfigs);
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
    }

    @Override
    public List<LoadedData> load(String serverCommand) {
        List<String> stack = inputStack(serverCommand);
        List<LoadedData> loadables = candidatePath(stack)
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
                commonConfigs.stream().flatMap(DefaultConfigurationLoader::suffixCandidates),
                configResolver.baseConfig(),
                stack.stream().flatMap(string ->
                        Stream.concat(
                                suffixCandidates(string),
                                configResolver.stackedConfig(string))
                )
        ).flatMap(Function.identity()).distinct();
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

    private void failOnEmpty(List<String> stack, List<LoadedData> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            configResolver.baseConfig().collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", stack) + "]" +
                            stack.stream()
                                    .flatMap(configResolver::stackedConfig)
                                    .collect(Collectors.joining(", ")) +
                            ", paths: " + candidatePath(stack).collect(Collectors.joining(", ")));
        }
    }

    private void logProgress(List<String> stack, List<LoadedData> loadables) {
        progressLogger.println(() -> getClass().getSimpleName() +
                ": Resolved config stack [" + String.join(", ", stack) + "] from paths:\n  " +
                loadables.stream().map(LoadedData::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    private static Stream<String> suffixCandidates(String name) {
        return Arrays.stream(Suffix.values()).map(suffix -> suffix.suffixed(name));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.configResolver + " <= " + delegateProvider + "]";
    }
}
