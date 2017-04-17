package no.scienta.alchemy.dropwizard.configstack;

import java.util.List;

/**
 * Responsible for providing the content for a {@link io.dropwizard.cli.ServerCommand server command}
 * argument.
 */
@FunctionalInterface
public interface ConfigurationLoader {

    /**
     * Retrieve the loadable resources requested by the server command.
     *
     * @param serverCommand The argument to the {@link io.dropwizard.cli.ServerCommand server command}
     * @return Loadable resources
     */
    List<LoadedData> load(String serverCommand);
}
