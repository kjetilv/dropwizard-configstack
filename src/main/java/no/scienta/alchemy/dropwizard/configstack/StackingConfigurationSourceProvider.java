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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class StackingConfigurationSourceProvider<C extends Configuration> implements ConfigurationSourceProvider {

    private final Bootstrap<C> bootstrap;

    private final JsonCombiner jsonCombiner;

    private final boolean variableReplacements;

    private final JsonReplacer.Replacer replacer;

    private final ProgressLogger progressLogger;

    private final LoadablesResolver<C> loadablesResolver;

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
        if (this.bootstrap.getConfigurationSourceProvider() instanceof StackingConfigurationSourceProvider) {
                throw new IllegalStateException
                        ("Please set a different source provider: " + this.bootstrap);
            }
        ConfigurationSourceProvider provider = this.bootstrap.getConfigurationSourceProvider();
        if (provider instanceof StackingConfigurationSourceProvider) {
            throw new IllegalStateException
                    ("Please set a different source provider: " + this.bootstrap.getConfigurationSourceProvider());
        }
        this.progressLogger = progressLogger == null ? System.out::println : progressLogger;
        this.loadablesResolver = new LoadablesResolver<>(
                provider,
                Objects.requireNonNull(resolver, "resolver"),
                this.progressLogger);

        this.jsonCombiner = jsonCombiner == null ? new JsonCombiner() : jsonCombiner;
        this.variableReplacements = variableReplacements;
        this.replacer = replacer;
    }

    @Override
    public InputStream open(String path) throws IOException {
        List<Loadable> prioritizedLoadables = loadablesResolver.resolveLoadables(path);

        JsonNode combined = combine(prioritizedLoadables);
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

    private JsonNode combine(List<Loadable> loadables) {
        return loadables.stream()
                .flatMap(this::readJson)
                .reduce(null, jsonCombiner::combine);
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
        return loadable.isYaml() ? new YAMLFactory(objectMapper) : objectMapper.getFactory();
    }

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + bootstrap.getApplication() + " <= " + this.loadablesResolver + "]";
    }
}
