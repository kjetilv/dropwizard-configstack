package no.scienta.alchemy.dropwizard.configstack;

import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BasenameVariationsResourceResolverTest {

    @Test
    public void testBase() {
        assertConfigs(
                resolver().baseResource(),
                "StackAppConfiguration");
    }

    @Test
    public void testStack() {
        assertConfigs(resolver().stackedResource("hurra"),
                "StackAppConfiguration-hurra");
    }

    private void assertConfigs(Stream<String> stream, String... expected) {
        List<String> configs = stream.collect(Collectors.toList());
        assertThat(configs.size(), is(expected.length));
        assertThat(configs, hasItems(expected));
    }

    private BasenameVariationsResourceResolver resolver() {
        return new BasenameVariationsResourceResolver(StackAppConfiguration.class);
    }
}
