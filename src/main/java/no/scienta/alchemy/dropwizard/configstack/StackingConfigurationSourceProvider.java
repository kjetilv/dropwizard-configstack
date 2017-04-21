package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

final class StackingConfigurationSourceProvider implements ConfigurationSourceProvider {

    private final ObjectMapper objectMapper;

    private final ConfigurationBuilder configurationBuilder;

    private final ConfigurationSubstitutor configurationSubstitutor;

    private final ConfigurationLoader configurationLoader;

    private final ProgressLogger progressLogger;

    StackingConfigurationSourceProvider(ConfigurationLoader configurationLoader,
                                        ConfigurationBuilder configurationBuilder,
                                        ConfigurationSubstitutor configurationSubstitutor,
                                        ObjectMapper objectMapper,
                                        ProgressLogger progressLogger) {
        this.configurationBuilder =
                Objects.requireNonNull(configurationBuilder, "configurationResolver");
        this.configurationSubstitutor =
                Objects.requireNonNull(configurationSubstitutor, "configurationSubstitutor");
        this.configurationLoader =
                Objects.requireNonNull(configurationLoader, "loadablesResolver");
        this.objectMapper =
                Objects.requireNonNull(objectMapper, "objectMapper");
        this.progressLogger =
                Objects.requireNonNull(progressLogger, "progressLogger");
    }

    /**
     * @param serverCommand The argument to the {@link io.dropwizard.cli.ServerCommand server command}.
     * @return Input stream with compiled data
     */
    @Override
    public InputStream open(String serverCommand) {
        JsonNode config = load(serverCommand);
        logResult(config);
        return stream(config);
    }

    private JsonNode load(String serverCommand) {
        Collection<LoadedData> loadedData = configurationLoader.load(serverCommand);
        JsonNode combinedConfiguration = configurationBuilder.build(loadedData);
        return configurationSubstitutor.substitute(combinedConfiguration);
    }

    private void logResult(JsonNode node) {
        progressLogger.println(() -> "End-result combined config: " + writeConfig(node));
    }

    private String writeConfig(JsonNode combined) {
        try {
            return objectMapper.writeValueAsString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write config tree: " + combined.toString(), e);
        }
    }

    private InputStream stream(JsonNode node) {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            objectMapper.writeValue(baos, node);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load data from " + node, e);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ignore) {
                }
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "]";
    }
}
