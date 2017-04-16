package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class StackingConfigurationSourceProvider<C extends Configuration> implements ConfigurationSourceProvider {

    private final Bootstrap<C> bootstrap;

    private final ArrayStrategy arrayStrategy;

    private final boolean substituteVariables;

    private final Substitutor substitutor;

    private final ProgressLogger progressLogger;

    private final LoadablesResolver<C> loadablesResolver;

    /**
     * @param bootstrap      The bootstrap
     * @param resolver       How to resolve base config and stacked elements
     * @param arrayStrategy  How to combine config arrays, may be null
     * @param progressLogger How to log progress, may be null
     */
    StackingConfigurationSourceProvider(Bootstrap<C> bootstrap,
                                        ConfigResolver<C> resolver,
                                        ArrayStrategy arrayStrategy,
                                        boolean substituteVariables,
                                        Substitutor substitutor,
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
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
        this.progressLogger = Objects.requireNonNull(progressLogger, "progressLogger");
        this.loadablesResolver = new LoadablesResolver<>(
                provider,
                Objects.requireNonNull(resolver, "resolver"),
                this.progressLogger);

        this.substituteVariables = substituteVariables;
        this.substitutor = substitutor;
    }

    @Override
    public InputStream open(String path) throws IOException {
        List<Loadable> prioritizedLoadables = getLoadables(path);

        JsonNode combined = combine(prioritizedLoadables);
        JsonNode processed = substituteVariables ? substitute(combined) : combined;

        progressLogger.println(() -> "Combined config: " + writeConfig(processed));

        return stream(processed);
    }

    private List<Loadable> getLoadables(String path) {
        return loadablesResolver.resolveLoadables(path);
    }

    private JsonNode combine(List<Loadable> loadables) {
        return loadables.stream()
                .flatMap(this::readJson)
                .reduce(null, JsonCombiner.create(arrayStrategy));
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

    private JsonNode substitute(JsonNode combined) {
        Substitutor replacer = this.substitutor != null ? this.substitutor
                : new DefaultSubstitutor(System.getProperties(), System.getenv(), combined);
        return JsonSubstitutor.substitute(combined, replacer);
    }

    private InputStream stream(JsonNode processed) throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bootstrap.getObjectMapper().writeValue(baos, processed);
        } finally {
            if (baos != null) {
                baos.close();
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private String writeConfig(JsonNode combined) {
        try {
            return bootstrap.getObjectMapper().writeValueAsString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write config tree: " + combined.toString(), e);
        }
    }

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + bootstrap.getApplication() + " <= " + this.loadablesResolver + "]";
    }
}
