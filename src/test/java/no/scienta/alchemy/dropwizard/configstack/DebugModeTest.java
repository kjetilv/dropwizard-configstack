package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DebugModeTest {

    @ClassRule
    public static DropwizardAppRule<StackAppConfiguration> rule =
            new DropwizardAppRule<>(StackApp.class, "debug");

    @Test
    public void startup() {
        assertThat(rule.getConfiguration().sub.mode, is("debug"));
    }
}
