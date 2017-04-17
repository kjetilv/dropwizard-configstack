package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Responsible for compiling a set of {@link LoadedData data} into a single {@link JsonNode}.
 */
@FunctionalInterface
public interface ConfigurationCombiner {

    /**
     * Responsible for combining all source data into an end-result JSON AST
     *
     * @param loadables Loadable data
     * @return Complete JSON AST
     */
    JsonNode compile(List<LoadedData> loadables);
}
