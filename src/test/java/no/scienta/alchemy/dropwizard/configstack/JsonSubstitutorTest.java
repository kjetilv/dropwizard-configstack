package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class JsonSubstitutorTest {

    @Test
    public void substituteSimple() throws Exception {
        JsonNode jn1 = read("{ \"foo\": \"bar\"}");
        JsonNode jnc = Json.substitute(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertEquals("barbar", jnc.get("foo").asText());
    }

    @Test
    public void substituteStructure() throws Exception {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": \"bar\"}}");
        JsonNode jnc = Json.substitute(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertEquals("barbar", jnc.get("foo").get("fooNested").asText());
    }

    @Test
    public void substituteArray() throws IOException {
        JsonNode jn1 = read("{ \"foo\": { \"fooNested\": [\"zip\", \"foo\"]}}");
        JsonNode jnc = Json.substitute(jn1, value -> value + value);

        assertNotNull(jnc);
        assertEquals(1, jnc.size());
        assertTrue(jnc.has("foo"));
        assertTrue(jnc.get("foo").isObject());
        assertTrue(jnc.get("foo").get("fooNested").isArray());
        assertEquals(2, jnc.get("foo").get("fooNested").size());
        assertEquals("zipzip", jnc.get("foo").get("fooNested").get(0).asText());
        assertEquals("foofoo", jnc.get("foo").get("fooNested").get(1).asText());
    }

    private static JsonNode read(String string) throws IOException {
        return new ObjectMapper().readTree(string);
    }
}
