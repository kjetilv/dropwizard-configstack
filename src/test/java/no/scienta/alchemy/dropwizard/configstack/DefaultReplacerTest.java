package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.Properties;

import static no.scienta.alchemy.dropwizard.configstack.JsonStuff.read;
import static org.junit.Assert.*;

public class DefaultReplacerTest {

    @Test
    public void replaceSimple() {
        JsonNode jn1 = read("{ \"foo\": \"bar${bar}\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonReplacer.replace(jn1, new DefaultReplacer(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(1, replace.size());
        assertTrue(replace.has("foo"));
        assertEquals("barzot", replace.get("foo").asText());
    }

    @Test
    public void replaceSimpleTwice() {
        JsonNode jn1 = read("{ \"foo\": \"bar${bar}-${bar}-\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonReplacer.replace(jn1, new DefaultReplacer(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(1, replace.size());
        assertTrue(replace.has("foo"));
        assertEquals("barzot-zot-", replace.get("foo").asText());
    }

    @Test
    public void replacePointed() {
        JsonNode jn1 = read("{ \"foo\": \"bar\", \"zot\": \"${/foo}\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonReplacer.replace(jn1, new DefaultReplacer(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(2, replace.size());
        assertTrue(replace.has("zot"));
        assertEquals("bar", replace.get("zot").asText());
    }
}
