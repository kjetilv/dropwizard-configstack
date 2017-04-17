package no.scienta.alchemy.dropwizard.configstack.test;

import io.dropwizard.testing.junit.DropwizardAppRule;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackApp;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProdModeTest {

    @ClassRule
    public static DropwizardAppRule<StackAppConfiguration> rule =
            new DropwizardAppRule<>(StackApp.class, "secured,prod");

    @Test
    public void startup() {
        StackAppConfiguration configuration = rule.getConfiguration();
        assertThat(configuration.sub.mode, is("prod"));
        assertThat(configuration.sub.remoteURI, is(URI.create("http://127.0.0.1")));
    }
}
