package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

final class StackingConfigurationSourceProvider implements ConfigurationSourceProvider {

    private final ObjectMapper objectMapper;

    private final ConfigurationStacker configurationStacker;

    private final ConfigurationLoader configurationLoader;

    private final ConfigurationAssembler configurationAssembler;

    private final ConfigurationSubstitutor configurationSubstitutor;

    private final ProgressLogger progressLogger;

    StackingConfigurationSourceProvider(ConfigurationStacker configurationStacker,
                                        ConfigurationResourceResolver configurationResourceResolver,
                                        ConfigurationLoader configurationLoader,
                                        ConfigurationAssembler configurationAssembler,
                                        ConfigurationSubstitutor configurationSubstitutor,
                                        ObjectMapper objectMapper,
                                        ProgressLogger progressLogger) {
        this.configurationStacker =
                Objects.requireNonNull(configurationStacker, "configurationStacker");
        this.configurationLoader =
                Objects.requireNonNull(configurationLoader, "loadablesResolver");
        this.configurationAssembler =
                Objects.requireNonNull(configurationAssembler, "configurationResolver");
        this.configurationSubstitutor =
                Objects.requireNonNull(configurationSubstitutor, "configurationSubstitutor");
        this.objectMapper =
                Objects.requireNonNull(objectMapper, "objectMapper");
        this.progressLogger = safeLogger(
                Objects.requireNonNull(progressLogger, "progressLogger")
        );
    }

    /**
     * @param serverCommand The argument to the {@link io.dropwizard.cli.ServerCommand server command}.
     * @return Input stream with compiled data
     */
    @Override
    public InputStream open(String serverCommand) {
        return tryOpen(serverCommand).orElseThrow(notFound(serverCommand));
    }

    private Optional<InputStream> tryOpen(String serverCommand) {
        try {
            Optional<JsonNode> config = cmd(serverCommand)
                    .map(configurationStacker::parse)
                    .map(configurationLoader::load)
                    .map(configurationAssembler::assemble)
                    .map(configurationSubstitutor::substitute);
            config.ifPresent(this::logResult);
            return config.map(this::stream);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config from argument <" + serverCommand + ">", e);
        }
    }

    private ProgressLogger safeLogger(ProgressLogger progressLogger) {
        return info -> {
            try {
                progressLogger.accept(info);
            } catch (Exception e) {
                System.err.println("ERROR Logging <" + info.get() + ">: " + e);
            }
        };
    }

    private Optional<String> cmd(String serverCommand) {
        return Optional.ofNullable(serverCommand).filter(cmd -> !cmd.isEmpty());
    }

    private Supplier<IllegalArgumentException> notFound(String serverCommand) {
        return () -> new IllegalArgumentException("No config found for <" + serverCommand + ">");
    }

    private void logResult(JsonNode node) {
        try {
            progressLogger.println(() ->
                    "End-result combined config: " + writeConfig(node));
        } catch (Exception e) {
            progressLogger.println(() ->
                    "Failed to serialize config for logging: " + e);
        }
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
