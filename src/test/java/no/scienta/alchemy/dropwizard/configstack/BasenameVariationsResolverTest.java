package no.scienta.alchemy.dropwizard.configstack;

import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BasenameVariationsResolverTest {

    @Test
    public void testBase() {
        assertConfigs(
                resolver().baseConfig(),
                "StackAppConfiguration.json", "StackAppConfiguration.yaml");
    }

    @Test
    public void testStack() {
        assertConfigs(resolver().stackedConfig("hurra"),
                "StackAppConfiguration-hurra.json", "StackAppConfiguration-hurra.yaml");
    }

    private void assertConfigs(Stream<String> stream, String... expected) {
        List<String> configs = stream.collect(Collectors.toList());
        assertThat(configs.size(), is(Suffix.values().length));
        assertThat(configs, hasItems(expected));
    }

    private BasenameVariationsResolver resolver() {
        return new BasenameVariationsResolver(StackAppConfiguration.class);
    }
}
