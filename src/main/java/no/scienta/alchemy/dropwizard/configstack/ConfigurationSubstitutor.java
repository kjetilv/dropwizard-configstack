package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Responsible for performing all subsitutions in a loaded configuration.
 */
@FunctionalInterface
public interface ConfigurationSubstitutor {

    /**
     * Responsible for performing all substitutions.
     *
     * @param configuration Configuration
     * @return Substituted JSON AST
     */
    JsonNode substitute(JsonNode configuration);
}
