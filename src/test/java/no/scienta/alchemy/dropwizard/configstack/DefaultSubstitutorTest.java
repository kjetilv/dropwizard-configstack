package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

public class DefaultSubstitutorTest {

    @Test
    public void simple() throws IOException {
        JsonNode jn1 = read("{ \"foo\": \"bar${bar}\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonSubstitutor.substitute(jn1, new DefaultStringSubstitutor(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(1, replace.size());
        assertTrue(replace.has("foo"));
        assertEquals("barzot", replace.get("foo").asText());
    }

    @Test
    public void simpleTwice() throws IOException {
        JsonNode jn1 = read("{ \"foo\": \"bar${bar}-${bar}-\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonSubstitutor.substitute(jn1, new DefaultStringSubstitutor(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(1, replace.size());
        assertTrue(replace.has("foo"));
        assertEquals("barzot-zot-", replace.get("foo").asText());
    }

    @Test
    public void pointer() throws IOException {
        JsonNode jn1 = read("{ \"foo\": \"bar\", \"zot\": \"${/foo}\"}");
        Properties properties = new Properties();
        properties.setProperty("bar", "zot");
        JsonNode replace = JsonSubstitutor.substitute(jn1, new DefaultStringSubstitutor(properties, null, jn1));

        assertNotNull(replace);
        assertEquals(2, replace.size());
        assertTrue(replace.has("zot"));
        assertEquals("bar", replace.get("zot").asText());
    }

    private static JsonNode read(String string) throws IOException {
        return new ObjectMapper().readTree(string);
    }
}
