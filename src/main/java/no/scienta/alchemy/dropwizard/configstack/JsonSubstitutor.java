package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.Function;

import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.arrayNode;
import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.objectNode;
import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.textNode;

final class JsonSubstitutor {

    static JsonNode substitute(JsonNode base, Function<String, String> substitutor) {
        return JsonUtils.isNull(base) ? base
                : base.isTextual() ? substitutedText(base, substitutor)
                : base.isObject() ? object(base, substitutor)
                : base.isArray() ? array(base, substitutor)
                : base;
    }


    private static JsonNode array(JsonNode base, Function<String, String> substitutor) {
        ArrayNode arr = arrayNode();
        for (JsonNode el : base) {
            arr.add(substitute(el, substitutor));
        }
        return arr;
    }

    private static JsonNode object(JsonNode base, Function<String, String> substitutor) {
        ObjectNode obj = objectNode();
        base.fieldNames().forEachRemaining(name ->
                obj.set(name, substituted(base, name, substitutor)));
        return obj;
    }

    private static JsonNode substituted(JsonNode base, String name, Function<String, String> substitutor) {
        return substitute(base.get(name), substitutor);
    }

    private static JsonNode substitutedText(JsonNode base, Function<String, String> substitutor) {
        String original = base.textValue();
        String substituted = substitutor.apply(original);
        return substituted.equals(original) ? base : textNode(substituted);
    }
}
