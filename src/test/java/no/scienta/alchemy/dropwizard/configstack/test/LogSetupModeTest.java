package no.scienta.alchemy.dropwizard.configstack.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackApp;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogSetupModeTest {

    @ClassRule
    public static final DropwizardAppRule<StackAppConfiguration> rule =
            new DropwizardAppRule<>(StackApp.class, "logging,serverlogging,debug");

    @Test
    public void startup() {
        StackAppConfiguration configuration = rule.getConfiguration();
        assertThat(configuration.appName, is("StackTest"));

        assertThat(configuration.getLoggingFactory(), instanceOf(DefaultLoggingFactory.class));

        DefaultLoggingFactory dlf = (DefaultLoggingFactory) configuration.getLoggingFactory();
        assertThat(dlf.getLevel(), is(Level.INFO));

        AppenderFactory<ILoggingEvent> file = dlf.getAppenders().asList().get(0);
        assertThat(file, instanceOf(FileAppenderFactory.class));
    }
}
