package no.scienta.alchemy.dropwizard.configstack;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class DefaultConfigurationStackerTest {

    @Test
    public void testSimpleStack() {
        assertStack("foo,bar,zot", "foo", "bar", "zot");
    }

    @Test
    public void testStrangerStack() {
        assertStack("foo;bar]zot", "foo", "bar", "zot");
    }

    private void assertStack(String serverCommand, String... strings) {
        assertThat(new DefaultConfigurationStacker().parse(serverCommand),
                hasItems(strings));
    }
}
