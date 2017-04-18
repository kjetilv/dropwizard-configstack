package no.scienta.alchemy.dropwizard.configstack;

import java.util.stream.Stream;

/**
 * Responsible for mapping stack names (e.g. {@code debug}, {@code prod}) to actual, lookup-able resources.  The
 * values should be names that are available either as file names or classpath resources.  Resources may be returned
 * with or without a type (i.e. {@code conf} or {@code conf.yaml}) – unsuffixed names will be tried with all available
 * {@link Suffix suffixes}.
 */
public interface ConfigurationResourceResolver {

    /**
     * @return Resource(s) for base config.
     *
     */
    Stream<String> baseResource();

    /**
     * @param stack Stack element
     * @return Resource(s) for a stackable resource that overrides the {@link #baseResource()}.
     */
    Stream<String> stackedResource(String stack);
}
