package no.scienta.alchemy.dropwizard.configstack;

import java.util.Arrays;
import java.util.Collection;

/**
 * Responsible for providing the content for a {@link io.dropwizard.cli.ServerCommand server command}
 * argument.
 */
@FunctionalInterface
public interface ConfigurationLoader {

    default Collection<LoadedData> load(String... stack) {
        return load(Arrays.asList(stack));
    }

    /**
     * Retrieve the loadable resources requested by the server command.
     *
     * @param stack The arguments to the {@link io.dropwizard.cli.ServerCommand server command}
     * @return Loadable resources
     */
    Collection<LoadedData> load(Collection<String> stack);
}
