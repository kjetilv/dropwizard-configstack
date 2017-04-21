package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.objectNode;

final class DefaultConfigurationBuilder implements ConfigurationBuilder {

    private final ObjectMapper objectMapper;

    private final ArrayStrategy arrayStrategy;

    /**
     * @param objectMapper   Object mapper
     * @param arrayStrategy  How to combine config arrays, may be null
     */
    DefaultConfigurationBuilder(ObjectMapper objectMapper, ArrayStrategy arrayStrategy) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.arrayStrategy = Objects.requireNonNull(arrayStrategy, "arrayStrategy");
    }

    @Override
    public JsonNode build(Collection<LoadedData> loadables) {
        return loadables.stream()
                .flatMap(this::readJson)
                .reduce(objectNode(), JsonCombiner.create(arrayStrategy));
    }

    private Stream<JsonNode> readJson(LoadedData loadedData) {
        if (loadedData.hasContent()) {
            try {
                JsonFactory factory = factory(objectMapper, loadedData);
                JsonNode jsonNode = factory.createParser(loadedData.getStream()).readValueAsTree();
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