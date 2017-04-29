package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.Map;
import java.util.Properties;

final class DefaultStringSubstitutor implements StringSubstitutor {

    private final Properties properties;

    private final Map<String, String> env;

    private final JsonNode node;

    DefaultStringSubstitutor(Properties properties, Map<String, String> env, JsonNode node) {
        this.properties = properties;
        this.env = env;
        this.node = node;
    }

    @Override
    public String substitute(String value) {
        String workString = value;
        while (true) {
            StrSubstitutor strSubstitutor = new StrSubstitutor(new Lookup(node), "${", "}", '\'');
            String replaced = strSubstitutor.replace(workString);
            if (replaced.equals(workString)) {
                return workString;
            }
            workString = replaced;
        }
    }

    private final class Lookup extends StrLookup<String> {

        private final JsonNode node;

        private Lookup(JsonNode node) {
            this.node = node;
        }

        @Override
        public String lookup(String key) {
            return properties != null && properties.getProperty(key) != null ? properties.getProperty(key)
                    : env != null && env.containsKey(key) ? env.get(key)
                    : resolveJsonPointer(key, pointer(key)).asText();
        }

        private JsonNode resolveJsonPointer(String key, JsonPointer pointer) {
            TreeNode pointed = ((TreeNode) node).at(pointer);
            return pointed == null || pointed.isMissingNode() || !pointed.isValueNode()
                    ? fail(key, "Unknown system property, env variable or JSON node")
                    : (JsonNode) pointed;
        }

        private JsonPointer pointer(String key) {
            try {
                return JsonPointer.compile(key);
            } catch (IllegalArgumentException e) {
                return fail(key, "Unknown system property or env variable, and invalid JSON pointer");
            }
        }

        private <T> T fail(String key, String problem) {
            throw new IllegalArgumentException("Unknown variable reference '" + key + "': " + problem);
        }
    }
}
