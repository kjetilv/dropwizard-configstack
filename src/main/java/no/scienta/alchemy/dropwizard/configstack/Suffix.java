package no.scienta.alchemy.dropwizard.configstack;

import java.util.Arrays;

enum Suffix {

    JSON, YAML;

    static boolean anySuffix(String path) {
        return Arrays.stream(values()).anyMatch(suffix -> suffix.isSuffixed(path));
    }

    boolean isSuffixed(String path) {
        return path != null && path.toLowerCase().endsWith("." + name().toLowerCase());
    }

    String suffixed(String path) {
        return anySuffix(path) ? path : path + "." + name().toLowerCase();
    }
}
