package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.IntStream;

import static no.scienta.alchemy.dropwizard.configstack.ArrayStrategy.*;
import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.*;

final class Json {

    static BinaryOperator<JsonNode> combiner(ArrayStrategy arrayStrategy) {
        return arrayStrategy == null
                ? (base, override) -> Combi.combine(base, override, null)
                : (j1, j2) -> Combi.combine(j1, j2, arrayStrategy);
    }

    static JsonNode substitute(JsonNode base, Function<String, String> substitutor) {
        return JsonUtils.isNull(base) ? base
                : base.isTextual() ? Subs.substitutedText(base, substitutor)
                : base.isObject() ? Subs.object(base, substitutor)
                : base.isArray() ? Subs.array(base, substitutor)
                : base;
    }

    private static final class Combi {

        /**
         * Return a new node based on the base, with override on top.
         *
         * @param base     The base
         * @param override The override
         * @return New, combined node
         */
        private static JsonNode combine(JsonNode base, JsonNode override, ArrayStrategy arrayStrategy) {
            return isNull(base) ? override
                    : isNull(override) ? base
                    : isObject(base, override) ? combineObject((ObjectNode) base, (ObjectNode) override, arrayStrategy)
                    : isArray(base, override) ? combineArray((ArrayNode) base, (ArrayNode) override, arrayStrategy)
                    : override;
        }

        private static ObjectNode combineObject(ObjectNode base, ObjectNode override, ArrayStrategy arrays) {
            ObjectNode copy = base.deepCopy();
            override.fieldNames().forEachRemaining(name ->
                    copy.set(name, combine(base.get(name), override.get(name), arrays)));
            return copy;
        }

        private static ArrayNode combineArray(ArrayNode base, ArrayNode override, ArrayStrategy arrays) {
            return arrays == null || arrays == OVERLAY ? overlay(base, override, arrays)
                    : arrays == REPLACE ? override
                    : sequence(base, override, arrays == APPEND);
        }

        private static ArrayNode overlay(ArrayNode base, ArrayNode override, ArrayStrategy arrays) {
            return indexes(base, override)
                    .mapToObj(index ->
                            combine(base.get(index), override.get(index), arrays))
                    .reduce(JsonNodeFactory.instance.arrayNode(), ArrayNode::add, ArrayNode::addAll);
        }

        private static ArrayNode sequence(ArrayNode base, ArrayNode override, boolean append) {
            ArrayNode node = arrayNode();
            node.addAll(append ? base : override);
            node.addAll(append ? override : base);
            return node;
        }

        private static IntStream indexes(JsonNode base, JsonNode override) {
            return IntStream.range(0, Math.max(base.size(), override.size()));
        }
    }

    private static final class Subs {

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
}
