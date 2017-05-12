package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;

import java.io.IOException;

public interface BundleFondle {

    default <C extends Configuration> C read(Class<C> configClass, ObjectMapper objectMapper)
            throws IOException {
        return read(configClass, null, objectMapper);
    }

    <C extends Configuration> C read(Class<C> configClass, String path, ObjectMapper objectMapper)
            throws IOException;
}
