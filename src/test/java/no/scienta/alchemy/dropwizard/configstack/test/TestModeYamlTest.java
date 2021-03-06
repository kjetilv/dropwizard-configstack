package no.scienta.alchemy.dropwizard.configstack.test;

import io.dropwizard.testing.junit.DropwizardAppRule;
import no.scienta.alchemy.dropwizard.configstack.app.StackApp;
import no.scienta.alchemy.dropwizard.configstack.app.StackAppConfiguration;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestModeYamlTest {

    @ClassRule
    public static final DropwizardAppRule<StackAppConfiguration> rule =
            new DropwizardAppRule<>(StackApp.class, "test");

    @Test
    public void startup() {
        StackAppConfiguration configuration = rule.getConfiguration();
        assertThat(configuration.sub.mode, is("test"));
        assertThat(configuration.sub.remoteURI, is(URI.create("http://0.0.0.0:8080/find/me/here")));
    }
}
