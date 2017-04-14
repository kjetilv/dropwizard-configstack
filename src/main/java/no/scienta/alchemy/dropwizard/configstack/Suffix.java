package no.scienta.alchemy.dropwizard.configstack;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

enum Suffix {

    JSON, YAML;

    boolean isSuffixed(String path) {
        return path != null && path.toLowerCase().endsWith("." + name().toLowerCase());
    }

    String suffixed(String path) {
        return path + "." + name().toLowerCase();
    }

    static String unsuffixed(String path) {
        return suffixOf(path).map(suffix -> path.substring(0, path.length() - suffix.name().length() - 1)).orElse(path);
    }

    static Optional<Suffix> suffixOf(String path) {
        return suffixes().filter(suffix -> suffix.isSuffixed(path)).findAny();
    }

    static boolean anySuffix(String path) {
        return suffixes().anyMatch(suffix -> suffix.isSuffixed(path));
    }

    private static Stream<Suffix> suffixes() {
        return Arrays.stream(values());
    }
}
