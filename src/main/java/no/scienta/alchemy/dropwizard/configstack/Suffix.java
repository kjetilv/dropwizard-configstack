package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

enum Suffix {

    JSON, YAML;

    boolean isSuffixed(String path) {
        return path != null && path.toLowerCase().endsWith("." + name().toLowerCase());
    }

    String suffixed(Class<? extends Configuration> type) {
        return suffixed(type.getSimpleName());
    }

    String suffixed(String path) {
        return anySuffix(path) ? path : path + "." + name().toLowerCase();
    }

    static boolean anySuffix(String path) {
        return suffixes().anyMatch(suffix -> suffix.isSuffixed(path));
    }

    private static Stream<Suffix> suffixes() {
        return Arrays.stream(values());
    }
}
