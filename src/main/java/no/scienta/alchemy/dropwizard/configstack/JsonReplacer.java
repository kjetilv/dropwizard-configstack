package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

class JsonReplacer {

    static JsonNode replace(JsonNode base, Replacer replacer) {
        return base == null || base.isNull() ? base
                : base.isTextual() ? replacedText(base, replacer)
                : base.isObject() ? object(base, replacer)
                : base.isArray() ? array(base, replacer)
                : base;
    }

    private static JsonNode array(JsonNode base, Replacer replacer) {
        ArrayNode arr = FACTORY.arrayNode();
        for (JsonNode el : base) {
            arr.add(replace(el, replacer));
        }
        return arr;
    }

    private static JsonNode object(JsonNode base, Replacer replacer) {
        ObjectNode obj = FACTORY.objectNode();
        for (Iterator<String> it = base.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            JsonNode replacedSub = replace(base.get(name), replacer);
            obj.set(name, replacedSub);
        }
        return obj;
    }

    private static JsonNode replacedText(JsonNode base, Replacer replacer) {
        String original = base.textValue();
        String replaced = replacer.replace(original);
        return replaced.equals(original)
                ? base
                : FACTORY.textNode(replaced);
    }

    @FunctionalInterface
    public interface Replacer {

        String replace(String value);
    }

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;
}
