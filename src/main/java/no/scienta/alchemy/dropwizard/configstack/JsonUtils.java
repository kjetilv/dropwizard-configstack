package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.function.Predicate;

final class JsonUtils {

    static boolean isNull(JsonNode... nodes) {
        return nodes.length == 1 && empty(nodes[0]) || all(EMPTY, nodes);
    }

    static boolean isObject(JsonNode... nodes) {
        return all(JsonNode::isObject, nodes);
    }

    static boolean isArray(JsonNode... nodes) {
        return all(JsonNode::isArray, nodes);
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

    private static final Predicate<JsonNode> EMPTY = JsonUtils::empty;

    private static boolean empty(JsonNode node) {
        return node == null || node.isNull();
    }

    private static boolean all(Predicate<JsonNode> test, JsonNode... nodes) {
        return nodes.length == 1 && test.test(nodes[0]) || Arrays.stream(nodes).allMatch(test);
    }

    private JsonUtils() {
    }
}
