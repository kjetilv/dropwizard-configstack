package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.*;

class JsonReplacer {

    static JsonNode replace(JsonNode base, Replacer replacer) {
        return JsonUtils.isNull(base) ? base
                : base.isTextual() ? replacedText(base, replacer)
                : base.isObject() ? object(base, replacer)
                : base.isArray() ? array(base, replacer)
                : base;
    }

    private static JsonNode array(JsonNode base, Replacer replacer) {
        ArrayNode arr = arrayNode();
        for (JsonNode el : base) {
            arr.add(replace(el, replacer));
        }
        return arr;
    }

    private static JsonNode object(JsonNode base, Replacer replacer) {
        ObjectNode obj = objectNode();
        base.fieldNames().forEachRemaining(name ->
                obj.set(name, replaced(base, name, replacer)));
        return obj;
    }

    private static JsonNode replaced(JsonNode base, String name, Replacer replacer) {
        return replace(base.get(name), replacer);
    }

    private static JsonNode replacedText(JsonNode base, Replacer replacer) {
        String original = base.textValue();
        String replaced = replacer.replace(original);
        return replaced.equals(original) ? base : textNode(replaced);
    }

    @FunctionalInterface
    public interface Replacer {

        String replace(String value);
    }
}
