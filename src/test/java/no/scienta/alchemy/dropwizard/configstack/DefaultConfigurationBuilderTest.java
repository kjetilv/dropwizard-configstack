package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultConfigurationBuilderTest {

    @Test
    public void testBuild() {
        JsonNode node = testBuild("logging.json", "serverlogging.json");
        JsonNode appenders = node.get("logging").get("appenders");
        assertThat(appenders.size(), is(2));
        assertThat(appenders.get(1).get("type").asText(), is("file"));
    }

    private JsonNode testBuild(String... paths) {
        DefaultConfigurationBuilder combiner =
                new DefaultConfigurationBuilder(new ObjectMapper(), ArrayStrategy.OVERLAY);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        List<LoadedData> data =
                Arrays.stream(paths)
                        .map(path -> LoadedData.create(path, contextClassLoader.getResourceAsStream(path)))
                        .collect(Collectors.toList());
        return combiner.build(data);
    }
}
