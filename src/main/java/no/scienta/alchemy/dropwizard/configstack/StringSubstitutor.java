package no.scienta.alchemy.dropwizard.configstack;

/**
 * Responsible for substituting variables in source strings.
 */
@FunctionalInterface
public interface StringSubstitutor {

    /**
     * @param value Source string with substitutions that should be be made
     * @return String with substitutions made.  If none were made, returns the original vaule.
     */
    String substitute(String value);
}
