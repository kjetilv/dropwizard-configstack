package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class DefaultConfigurationCombiner implements ConfigurationCombiner {

    private final ObjectMapper objectMapper;

    private final ArrayStrategy arrayStrategy;

    /**
     * @param objectMapper   Object mapper
     * @param arrayStrategy  How to combine config arrays, may be null
     */
    DefaultConfigurationCombiner(ObjectMapper objectMapper, ArrayStrategy arrayStrategy) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
    }

    @Override
    public JsonNode compile(List<LoadedData> loadables) {
        return loadables.stream()
                .flatMap(this::readJson)
                .reduce(null, JsonCombiner.create(arrayStrategy));
    }

    private Stream<JsonNode> readJson(LoadedData loadedData) {
        if (loadedData.hasContent()) {
            try {
                JsonFactory factory = factory(objectMapper, loadedData);
                JsonNode jsonNode =
                        factory.createParser(loadedData.getStream()).readValueAsTree();
                return Stream.of(jsonNode);
            } catch (Exception e) {
                throw new IllegalStateException(this + " failed to parse " + loadedData, e);
            }
        }
        return Stream.empty();
    }

    private JsonFactory factory(ObjectMapper objectMapper, LoadedData loadable) {
        return loadable.isYaml() ? new YAMLFactory(objectMapper) : objectMapper.getFactory();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "]";
    }
}
