package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.stream.IntStream;

import static no.scienta.alchemy.dropwizard.configstack.JsonCombiner.ArrayStrategy.*;
import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.arrayNode;
import static no.scienta.alchemy.dropwizard.configstack.JsonUtils.isNull;

@SuppressWarnings("WeakerAccess")
public class JsonCombiner {

    /**
     * How to combine a base array with and override array.
     */
    public enum ArrayStrategy {
        /**
         * Override's elements are appended to the base array
         */
        APPEND,
        /**
         * Override's elements are prepended to the base array
         */
        PREPEND,
        /**
         * Override is recursively imposed on the base array, element by element
         */
        OVERLAY,
        /**
         * Override replaces base array
         */
        REPLACE;
    }

    private final ArrayStrategy arrays;

    /**
     * Combiner with {@link ArrayStrategy#OVERLAY overlay} array strategy
     */
    public JsonCombiner() {
        this(null);
    }

    /**
     * @param arrays How to handle arrays.  If null, {@link ArrayStrategy#OVERLAY overlay} is used
     */
    public JsonCombiner(ArrayStrategy arrays) {
        this.arrays = arrays;
    }

    /**
     * Return a new node based on the base, with override on top.
     *
     * @param base     The base
     * @param override The override
     * @return New, combined node
     */
    public JsonNode combine(JsonNode base, JsonNode override) {
        return combine(base, override, this.arrays);
    }

    /**
     * Return a new node based on the base, with override on top.
     *
     * @param base     The base
     * @param override The override
     * @return New, combined node
     */
    public static JsonNode combine(JsonNode base, JsonNode override, ArrayStrategy arrays) {
        return isNull(base) ? override
                : isNull(override) ? base
                : areObjects(base, override) ? combineObject((ObjectNode) base, (ObjectNode) override, arrays)
                : areArrays(base, override) ? combineArray((ArrayNode) base, (ArrayNode) override, arrays)
                : override;
    }

    private static boolean areObjects(JsonNode base, JsonNode override) {
        return base.isObject() && override.isObject();
    }

    private static boolean areArrays(JsonNode base, JsonNode override) {
        return base.isArray() && override.isArray();
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
                : sequence(base, override, arrays);
    }

    private static ArrayNode overlay(ArrayNode base, ArrayNode override, ArrayStrategy arrays) {
        return indexes(base, override)
                .mapToObj(index ->
                        combine(base.get(index), override.get(index), arrays))
                .reduce(JsonNodeFactory.instance.arrayNode(), ArrayNode::add, ArrayNode::addAll);
    }

    private static ArrayNode sequence(ArrayNode base, ArrayNode override, ArrayStrategy arrays) {
        ArrayNode node = arrayNode();
        node.addAll(arrays == APPEND ? base : override);
        node.addAll(arrays == APPEND ? override : base);
        return node;
    }

    private static IntStream indexes(JsonNode base, JsonNode override) {
        return IntStream.range(0, Math.max(base.size(), override.size()));
    }
}
