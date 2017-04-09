package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static no.scienta.alchemy.dropwizard.configstack.JsonStuff.read;
import static org.junit.Assert.*;

public class JsonReplacerTest {

    @Test
    public void replaceSimple() throws Exception {
        JsonNode jn1 = read("{ \"foo\": \"bar\"}");
        JsonNode jnc = JsonReplacer.replace(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertEquals("barbar", jnc.get("foo").asText());
    }

    @Test
    public void replaceStructure() throws Exception {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": \"bar\"}}");
        JsonNode jnc = JsonReplacer.replace(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertEquals("barbar", jnc.get("foo").get("fooNested").asText());
    }

    @Test
    public void replaceArray() {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [\"zip\", \"foo\"]}}");
        JsonNode jnc = JsonReplacer.replace(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(2, jnc.get("foo").get("fooNested").size());
        assertEquals("zipzip", jnc.get("foo").get("fooNested").get(0).asText());
        assertEquals("foofoo", jnc.get("foo").get("fooNested").get(1).asText());
    }
}
