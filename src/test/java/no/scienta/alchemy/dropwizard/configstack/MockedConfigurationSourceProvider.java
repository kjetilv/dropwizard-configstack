package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class MockedConfigurationSourceProvider implements ConfigurationSourceProvider {

    private final String[] paths;

    MockedConfigurationSourceProvider(String... paths) {
        this.paths = paths;
    }

    @Override
    public InputStream open(String resource) throws IOException {
        return Arrays.stream(paths)
                .filter(resource::equals)
                .map(res -> {
                    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
                    return stream == null ? new ByteArrayInputStream("{}".getBytes()) : stream;
                })
                .findAny()
                .orElseThrow(() ->
                        new IllegalStateException("No resource " + resource + " found"));
    }
}
