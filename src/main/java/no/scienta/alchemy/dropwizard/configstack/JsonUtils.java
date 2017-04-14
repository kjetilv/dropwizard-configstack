package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

final class JsonUtils {

    static boolean isNull(JsonNode base) {
        return base == null || base.isNull();
    }

    static ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }

    static ArrayNode arrayNode() {
        return JsonNodeFactory.instance.arrayNode();
    }

    static TextNode textNode(String text) {
        return JsonNodeFactory.instance.textNode(text);
    }

    private JsonUtils() {
        // The stuff is private
    }
}
