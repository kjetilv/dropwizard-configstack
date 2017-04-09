package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;

import java.io.IOException;

class JsonStuff {
    static JsonNode read(String string) {
        try {
            return new ObjectMapper().readTree(string);
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
        return null;
    }
}
