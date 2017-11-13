package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static org.apache.commons.text.StrMatcher.stringMatcher;

final class DefaultStringSubstitutor implements StringSubstitutor, Function<String, String> {

    private final Properties properties;

    private final Map<String, String> env;

    private final JsonNode node;

    DefaultStringSubstitutor(Properties properties, Map<String, String> env, JsonNode node) {
        this.properties = properties;
        this.env = env;
        this.node = node;
    }

    @Override
    public String apply(String value) {
        StrSubstitutor strSubstitutor =
                new StrSubstitutor(new Lookup(node), stringMatcher("${"), stringMatcher("}"), '\'');
        String workString = value;
        while (true) {
            String replaced = strSubstitutor.replace(workString);
            if (replaced.equals(workString)) {
                return workString;
            }
            workString = replaced;
        }
    }

    @Override
    public String substitute(String value) {
        return apply(value);
    }

    private final class Lookup extends StrLookup<String> {

        private static final String OR = "||";

        private final JsonNode node;

        private Lookup(JsonNode node) {
            this.node = node;
        }

        @Override
        public String lookup(String key) {
            String lookupKey;
            String defaultValue;
            if (key.contains(OR)) {
                int endIndex = key.lastIndexOf(OR);
                lookupKey = key.substring(0, endIndex);
                defaultValue = key.substring(endIndex + OR.length());
            } else {
                lookupKey = key;
                defaultValue = null;
            }
            String resolved = resolve(lookupKey);
            if (resolved == null && defaultValue == null) {
                throw new IllegalArgumentException
                        ("Unknown variable ref '" + key + "': Unknown system property, env variable or JSON node, no default was specified");
            }
            return resolved == null ? defaultValue : resolved;
        }

        private String resolve(String lookupKey) {
            return properties != null && properties.getProperty(lookupKey) != null ? properties.getProperty(lookupKey)
                    : env != null && env.containsKey(lookupKey) ? env.get(lookupKey)
                    : resolveJsonPointer(asPointer(lookupKey));
        }

        private String resolveJsonPointer(JsonPointer pointer) {
            if (pointer == null) {
                return null;
            }
            TreeNode pointed = ((TreeNode) node).at(pointer);
            return pointed == null || pointed.isMissingNode() || !pointed.isValueNode()
                    ? null
                    : ((JsonNode) pointed).asText();
        }

        private JsonPointer asPointer(String key) {
            if (key == null) {
                return null;
            }
            try {
                return JsonPointer.compile(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
