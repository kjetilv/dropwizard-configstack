package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;

/**
 * Responsible for compiling a set of {@link LoadedData data} into a single {@link JsonNode}.
 */
@FunctionalInterface
public interface ConfigurationBuilder {

    /**
     * Responsible for combining all source data into an end-result JSON AST
     *
     * @param loadables Loadable data
     * @return Complete JSON AST
     */
    JsonNode build(Collection<LoadedData> loadables);
}
