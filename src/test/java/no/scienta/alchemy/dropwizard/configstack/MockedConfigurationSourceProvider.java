package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockedConfigurationSourceProvider implements ConfigurationSourceProvider {

    private final ObjectMapper objectMapper;

    private final String[] paths;

    private final Map<String, String> contents = new ConcurrentHashMap<>();

    public MockedConfigurationSourceProvider(String... paths) {
        this(null, paths);
    }

    public MockedConfigurationSourceProvider(ObjectMapper objectMapper, String... paths) {
        this.objectMapper = objectMapper;
        this.paths = paths;
    }

    MockedConfigurationSourceProvider content(String path, StackAppConfiguration config) {
        try {
            contents.put(path, Optional.ofNullable(objectMapper)
                    .orElseThrow(() -> new IllegalStateException("No object mapper in " + this))
                    .writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Bad test data: " + config, e);
        }
        return this;
    }

    @Override
    public InputStream open(String resource) throws IOException {
        return Optional.ofNullable(contents.get(resource))
                .map(String::getBytes)
                .<InputStream>map(ByteArrayInputStream::new)
                .orElseGet(() -> Arrays.stream(paths)
                        .filter(resource::equals)
                        .map(res -> {
                            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
                            return stream == null ? new ByteArrayInputStream("{}".getBytes()) : stream;
                        })
                        .findAny().orElseThrow(() ->
                                new IllegalStateException("No resource " + resource + " found")));
    }
}
