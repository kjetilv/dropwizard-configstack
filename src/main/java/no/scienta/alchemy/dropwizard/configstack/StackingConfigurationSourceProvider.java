package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class StackingConfigurationSourceProvider implements ConfigurationSourceProvider {

    private final ObjectMapper objectMapper;

    private final ConfigurationCombiner configurationCombiner;

    private final ConfigurationSubstitutor configurationSubstitutor;

    private final ConfigurationLoader configurationLoader;

    private final ProgressLogger progressLogger;

    StackingConfigurationSourceProvider(ConfigurationLoader configurationLoader,
                                        ConfigurationCombiner configurationCombiner,
                                        ConfigurationSubstitutor configurationSubstitutor,
                                        ObjectMapper objectMapper,
                                        ProgressLogger progressLogger) {
        this.configurationCombiner =
                Objects.requireNonNull(configurationCombiner, "configurationResolver");
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
        JsonNode completed = load(serverCommand);
        logResult(completed);
        return stream(completed);
    }

    private JsonNode load(String serverCommand) {
        Collection<LoadedData> loadedData = configurationLoader.load(serverCommand);
        JsonNode combinedConfiguration = configurationCombiner.compile(loadedData);
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
