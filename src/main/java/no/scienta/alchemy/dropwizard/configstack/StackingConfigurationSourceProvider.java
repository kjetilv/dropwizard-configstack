package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class StackingConfigurationSourceProvider<C extends Configuration> implements ConfigurationSourceProvider {

    private final Bootstrap<C> bootstrap;

    private final ConfigResolver<C> resolver;

    private final JsonCombiner jsonCombiner;

    private final boolean variableReplacements;

    private final JsonReplacer.Replacer replacer;

    private final ProgressLogger progressLogger;

    private final ConfigurationSourceProvider provider;

    /**
     * @param bootstrap      The bootstrap
     * @param resolver       How to resolve base config and stacked elements
     * @param jsonCombiner   How to combine json structures, may be null
     * @param progressLogger How to log progress, may be null
     */
    StackingConfigurationSourceProvider(Bootstrap<C> bootstrap,
                                        ConfigResolver<C> resolver,
                                        JsonCombiner jsonCombiner,
                                        boolean variableReplacements,
                                        JsonReplacer.Replacer replacer,
                                        ProgressLogger progressLogger) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.provider = this.bootstrap.getConfigurationSourceProvider();
        if (this.provider instanceof StackingConfigurationSourceProvider) {
            throw new IllegalStateException
                    ("Please set a different source provider: " + this.bootstrap.getConfigurationSourceProvider());
        }

        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.jsonCombiner = jsonCombiner == null ? new JsonCombiner() : jsonCombiner;
        this.variableReplacements = variableReplacements;
        this.replacer = replacer;
        this.progressLogger = progressLogger == null ? System.out::println : progressLogger;
    }

    @Override
    public InputStream open(String path) throws IOException {
        String[] stack = stackedElements(path);
        JsonNode combined = readCombinedConfig(stack);
        JsonNode processed = variableReplacements ? replace(combined) : combined;

        String config = writeConfig(processed);
        progressLogger.println("Combined config:\n\n  " + config.replaceAll("\n", "\n  "));
        return new ByteArrayInputStream(config.getBytes(UTF_8));
    }

    private JsonNode replace(JsonNode combined) {
        JsonReplacer.Replacer replacer = this.replacer == null
                ? new DefaultReplacer(System.getProperties(), System.getenv(), combined)
                : this.replacer;
        return JsonReplacer.replace(combined, replacer);
    }

    private JsonNode readCombinedConfig(String[] stack) throws IOException {
        List<String> paths = paths(stack).collect(Collectors.toList());
        List<Loadable> loadables = paths.stream().flatMap(this::read)
                .collect(Collectors.toList());
        audit(stack, paths, loadables);

        return combine(loadables);
    }

    private JsonNode combine(List<Loadable> loadables) {
        return loadables.stream()
                .flatMap(this::readJson)
                .reduce(null, jsonCombiner::combine);
    }

    private void audit(String[] stack, List<String> paths, List<Loadable> loadables) {
        if (loadables.isEmpty()) {
            throw new IllegalStateException(
                    "Warning: No configs found for " +
                            resolver.baseConfig(this.bootstrap).collect(Collectors.joining(", ")) +
                            ", stack [" + String.join(", ", Arrays.asList(stack)) + "]" +
                            Arrays.stream(stack).flatMap(path ->
                                    resolver.stackedConfig(this.bootstrap, path)
                            ).collect(Collectors.joining(", ")) +
                            ", paths: " + String.join(", ", paths));
        }

        progressLogger.println(getClass().getSimpleName() +
                ": Loading config stack [" + String.join(", ", Arrays.asList(stack)) + "] from paths:\n  " +
                loadables.stream().map(Loadable::toString).collect(Collectors.joining("\n  ")) +
                "\n");
    }

    private String writeConfig(JsonNode combined) throws JsonProcessingException {
        return bootstrap.getObjectMapper().writeValueAsString(combined);
    }

    private Stream<JsonNode> readJson(Loadable loadable) {
        if (loadable.hasContent()) {
            ObjectMapper objectMapper = bootstrap.getObjectMapper();
            try {
                JsonFactory factory = factory(objectMapper, loadable);
                JsonNode jsonNode = factory.createParser(loadable.getStream()).readValueAsTree();
                return Stream.of(jsonNode);
            } catch (Exception e) {
                throw new IllegalStateException(this + " failed to load " + loadable, e);
            }
        }
        return Stream.empty();
    }

    private JsonFactory factory(ObjectMapper objectMapper, Loadable loadable) {
        return loadable.is(Suffix.YAML) ? new YAMLFactory(objectMapper) : objectMapper.getFactory();
    }

    private Stream<String> paths(String[] stack) {
        return Stream.of(
                resolver.commonConfig(bootstrap),
                resolver.baseConfig(bootstrap),
                Arrays.stream(stack).flatMap(string -> Stream.concat(
                        Stream.of(string),
                        resolver.stackedConfig(bootstrap, string)))
        ).flatMap(Function.identity());
    }

    private Stream<Loadable> read(String path) {
        try {
            return Stream.of(provider.open(path)).filter(Objects::nonNull).map(Loadable.forPath(path));
        } catch (FileNotFoundException e) {
            return Stream.empty();
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to open <" + path + ">", e);
        }
    }

    private String[] stackedElements(String input) {
        return Arrays.stream(input.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(s -> !s.trim().isEmpty())
                .toArray(String[]::new);
    }

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + bootstrap.getApplication() + " <= " + this.provider + "]";
    }
}
