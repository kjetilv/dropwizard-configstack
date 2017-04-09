package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.stream.IntStream;

import static no.scienta.alchemy.dropwizard.configstack.JsonCombiner.ArrayStrategy.*;

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
         * Override is recursively imposed on the base array, element by element.
         */
        OVERLAY,

        /**
         * Override replaces base array
         */
        REPLACE;

        public static ArrayStrategy DEFAULT = OVERLAY;
    }

    private final ArrayStrategy arrays;

    public JsonCombiner() {
        this(null);
    }

    public JsonCombiner(ArrayStrategy arrays) {
        this.arrays = arrays == null ? DEFAULT : arrays;
    }

    /**
     * Return a new node based on the base, override on top.
     *
     * @param base The base
     * @param override The override
     * @return Combined node
     */
    public JsonNode combine(JsonNode base, JsonNode override) {
        if (base == null || base.isNull()) {
            return override; // No base value, override wins
        }
        if (override == null || override.isNull()) {
            return base; // No override value, base wins
        }
        if (base.isObject() && override.isObject()) {
            return combineObject(base, override);
        }
        if (base.isArray() && override.isArray()) {
            return combineArray(base, override);
        }
        return override; // Override is leaf, wins
    }

    private JsonNode combineObject(JsonNode base, JsonNode override) {
        ObjectNode copy = base.deepCopy();
        override.fieldNames().forEachRemaining(name -> copy.set(name, combine(base.get(name), override.get(name))));
        return copy;
    }

    private JsonNode combineArray(JsonNode base, JsonNode override) {
        return arrays == REPLACE ? override :
                arrays == OVERLAY ? overlay(base, override)
                : sequence(base, override, arrays == APPEND);
    }

    private JsonNode overlay(JsonNode base, JsonNode override) {
        return IntStream.range(0, Math.max(base.size(), override.size()))
                .mapToObj(i -> combine(base.get(i), override.get(i)))
                .reduce(JsonNodeFactory.instance.arrayNode(), ArrayNode::add, ArrayNode::addAll);
    }

    private JsonNode sequence(JsonNode base, JsonNode override, boolean append) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        node.addAll((ArrayNode) (append ? base : override));
        node.addAll((ArrayNode) (append ? override : base));
        return node;
    }
}
