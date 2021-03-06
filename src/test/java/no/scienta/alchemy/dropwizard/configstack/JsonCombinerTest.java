package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class JsonCombinerTest {

    @Test
    public void combineSimple() throws Exception {
        JsonNode jn1 = read("{ \"foo\": \"bar\"}");
        JsonNode jn2 = read("{ \"zot\": \"zip\"}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertNotNull(jnc);
        assertEquals(2, jnc.size());
        assertTrue(jnc.has("foo"));
        assertEquals("bar", jnc.get("foo").asText());
        assertTrue(jnc.has("zot"));
        assertEquals("zip", jnc.get("zot").asText());
    }

    @Test
    public void nullOverride() throws IOException {
        JsonNode jn1 = read("{ \"foo\": \"bar\"}");
        JsonNode jn2 = read("{}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertEquals("bar", jnc.get("foo").asText());
    }

    @Test
    public void combineStructure() throws Exception {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": \"bar\"}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": \"zip\"}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertEquals("zip", jnc.get("foo").get("fooNested").asText());
    }

    @Test
    public void addStructure() throws IOException {
        JsonNode jn1 = read("{ \"bar\": 1 }");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": \"zip\"}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertNotNull(jnc);
        assertEquals(2, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertEquals("zip", jnc.get("foo").get("fooNested").asText());

        assertTrue(jnc.has("bar"));
        assertEquals(1, jnc.get("bar").asInt());
    }

    @Test
    public void addStructureField() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": \"bar\"}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": \"zip\", \"foo\": 1}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertEquals("zip", jnc.get("foo").get("fooNested").asText());

        assertTrue(jnc.get("foo").has("foo"));
        assertTrue(jnc.get("foo").get("foo").isInt());
        assertEquals(1, jnc.get("foo").get("foo").asInt());
    }

    @Test
    public void overlayArray() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [\"zip\"]}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": [\"zip\", \"zot\"]}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(2, jnc.get("foo").get("fooNested").size());
        assertEquals("zip", jnc.get("foo").get("fooNested").get(0).asText());
        assertEquals("zot", jnc.get("foo").get("fooNested").get(1).asText());
    }

    @Test
    public void appendArray() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [\"zip\"]}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": [\"zip\", \"zot\"]}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, ArrayStrategy.APPEND);

        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(3, jnc.get("foo").get("fooNested").size());
        assertEquals("zip", jnc.get("foo").get("fooNested").get(0).asText());
        assertEquals("zip", jnc.get("foo").get("fooNested").get(1).asText());
        assertEquals("zot", jnc.get("foo").get("fooNested").get(2).asText());
    }

    @Test
    public void prependArray() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [\"zip\"]}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": [\"zip\", \"zot\"]}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, ArrayStrategy.PREPEND);

        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(3, jnc.get("foo").get("fooNested").size());
        assertEquals("zip", jnc.get("foo").get("fooNested").get(0).asText());
        assertEquals("zot", jnc.get("foo").get("fooNested").get(1).asText());
        assertEquals("zip", jnc.get("foo").get("fooNested").get(2).asText());
    }

    @Test
    public void combineArray() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [{\"foo\":\"zip\"}, {\"zip\":\"zot\"}]}}");
        JsonNode jn2 = read("{ \"foo\": { \"fooNested\": [{\"foo\":\"zot\", \"bar\": \"zot\"}]}}");
        JsonNode jnc = JsonCombiner.combine(jn1, jn2, null);

        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(2, jnc.get("foo").get("fooNested").size());

        ArrayNode a1 = jnc.get("foo").get("fooNested").deepCopy();

        assertEquals("zot", a1.get(0).get("foo").asText());
        assertEquals("zot", a1.get(0).get("bar").asText());

        assertEquals("zot", a1.get(1).get("zip").asText());
    }

    private static JsonNode read(String string) throws IOException {
        return new ObjectMapper().readTree(string);
    }
}
