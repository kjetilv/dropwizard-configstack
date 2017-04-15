package no.scienta.alchemy.dropwizard.configstack;

/**
 * How to substitute string values
 */
@FunctionalInterface
public interface Substitutor {

    /**
     * @param value Value with substitutions that should be be made
     * @return Value with substitutions made.  If none were made, return the original vaule.
     */
    String subsitute(String value);
}
