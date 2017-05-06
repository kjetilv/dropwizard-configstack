package no.scienta.alchemy.dropwizard.configstack;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class StackingConfigurationSourceProviderTest {

    @Test
    public void testEmptyInput() throws IOException {
        ConfigurationSourceProvider provider = nullProvider();
        try {
            fail(provider.open("") + " not expected");
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Test
    public void testNullInput() throws IOException {
        ConfigurationSourceProvider provider = nullProvider();
        InputStream open = null;
        try {
            fail(provider.open(null) + " not expected");
        } catch (IllegalArgumentException ignore) {
        }
    }

    private ConfigurationSourceProvider nullProvider() {
        return new StackingConfigurationSourceProvider(
                serverCommand -> {
                    throw new UnsupportedOperationException();
                },
                new ConfigurationResourceResolver() {
                    @Override
                    public Stream<String> baseResource() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Stream<String> stackedResource(String stackedElement) {
                        throw new UnsupportedOperationException();
                    }
                },
                stack -> {
                    throw new UnsupportedOperationException();
                },
                loadables -> {
                    throw new UnsupportedOperationException();
                },
                configuration -> {
                    throw new UnsupportedOperationException();
                },
                new ObjectMapper(),
                info -> {
                    throw new UnsupportedOperationException();
                }
        );
    }

}
