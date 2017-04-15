package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.util.Arrays;
import java.util.Comparator;
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
        this.progressLogger = progressLogger == null ? System.out::println : progressLogger;
    }

    List<Loadable> resolveLoadables(String path) {
        String[] stack = stackedElements(path);
        List<String> paths = paths(stack);
        List<Loadable> loadables = prioritize(read(paths), stack);
        audit(stack, paths, loadables);
        return loadables;
    }

    private List<Loadable> read(List<String> paths) {
        return paths.stream().flatMap(this::read).collect(Collectors.toList());
    }

    private List<Loadable> prioritize(List<Loadable> loadables, String... stack) {
        Comparator<Loadable> priority = priority(stack);
        Stream<Loadable> sorted = loadables.stream().sorted(priority);
        return sorted.collect(Collectors.toList());
    }

    private void audit(String[] stack, List<String> paths, List<Loadable> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            resolver.baseConfig().collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", Arrays.asList(stack)) + "]" +
                            Arrays.stream(stack)
                                    .flatMap(resolver::stackedConfig)
                                    .collect(Collectors.joining(", ")) +
                            ", paths: " + String.join(", ", paths));
        }

        progressLogger.println(getClass().getSimpleName() +
                ": Resolved config stack [" + String.join(", ", Arrays.asList(stack)) + "] from paths:\n  " +
                loadables.stream().map(Loadable::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    private List<String> paths(String[] stack) {
        return Stream.of(
                resolver.commonConfig(),
                resolver.baseConfig(),
                Arrays.stream(stack).flatMap(string -> Stream.concat(
                        Arrays.stream(Suffix.values()).map(suff -> suff.suffixed(string)),
                        resolver.stackedConfig(string)))
        ).flatMap(Function.identity()).distinct().collect(Collectors.toList());
    }

    private Stream<Loadable> read(String path) {
        try {
            return Stream.of(provider.open(path)).filter(Objects::nonNull).map(Loadable.forPath(path));
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private String[] stackedElements(String input) {
        return Arrays.stream(input.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(s -> !s.trim().isEmpty())
                .toArray(String[]::new);
    }

    private Comparator<Loadable> priority(String... stack) {
        return (l1, l2) -> position(l1) < position(l2) ? -1
                : position(l1) > position(l2) ? 1
                : 0;
    }

    private int position(Loadable loadable, String... stack) {
        for (int i = 0; i < stack.length; i++) {
            if (Suffix.anySuffix(loadable.getPath())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.resolver + " <= " + provider + "]";
    }
}
