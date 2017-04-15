package no.scienta.alchemy.dropwizard.configstack;

/**
 * How to combine a base array with an override array.
 */
public enum ArrayStrategy {

    /**
     * Override's elements are appended to the base array
     */
    APPEND,
    /**
     * Override's elements are prepended to the base array
     */
    PREPEND,
    /**
     * Override is recursively imposed on the base array, element by element
     */
    OVERLAY,
    /**
     * Override replaces base array
     */
    REPLACE
}
