package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MiscModeTest {

    static {
        System.setProperty("me", "hubba");
    }

    @ClassRule
    public static DropwizardAppRule<StackAppConfiguration> rule =
            new DropwizardAppRule<>(StackApp.class, "prod,misc");

    @Test
    public void startup() {
        StackAppConfiguration configuration = rule.getConfiguration();
        assertThat(configuration.sub.mode, is("replace-hubba"));
        assertThat(configuration.sub.bar, is(100));
        assertThat(configuration.size, is(100));
    }
}
