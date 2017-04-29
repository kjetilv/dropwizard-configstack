package no.scienta.alchemy.dropwizard.configstack;

import java.util.Collection;

@FunctionalInterface
public interface ConfigurationStacker {

    /**
     * @param serverCommand The server command
     * @return The stack we've parsed from the server command
     */
    Collection<String> parse(String serverCommand);
}
