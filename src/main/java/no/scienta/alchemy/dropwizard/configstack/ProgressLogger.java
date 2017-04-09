package no.scienta.alchemy.dropwizard.configstack;

/**
 * A way for the provider to log what's going on.  Keep in mind that the provider operates
 * during startup, before logging is configured.
 */
@FunctionalInterface
public interface ProgressLogger {

    /**
     * @param info Something that happened
     */
    void println(String info);
}
