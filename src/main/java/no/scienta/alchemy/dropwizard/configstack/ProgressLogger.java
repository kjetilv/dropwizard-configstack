package no.scienta.alchemy.dropwizard.configstack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A way for the provider to log what's going on.  Keep in mind that the provider operates
 * during startup, before logging is configured.
 */
@FunctionalInterface
public interface ProgressLogger extends Consumer<Supplier<String>> {

    @Override
    void accept(Supplier<String> info);

    /**
     * @param info Something that happened
     */
    default void println(Supplier<String> info) {
        accept(info);
    }
}
