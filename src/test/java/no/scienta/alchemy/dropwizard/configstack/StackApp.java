package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackApp extends Application<StackAppConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(StackApp.class);

    @Override
    public void initialize(Bootstrap<StackAppConfiguration> bootstrap) {

        bootstrap.getObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .enable(JsonParser.Feature.IGNORE_UNDEFINED)
                .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
                .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
                .enable(SerializationFeature.INDENT_OUTPUT);

        bootstrap.addBundle(
                ConfigStackBundler.defaults(StackAppConfiguration.class)
                        .addCommonConfig("common-config", "stuff.json")
                        .setProgressLogger(string ->
                                System.out.println("### Just testing: " + string.get()))
                        .bundle());
    }

    @Override
    public void run(StackAppConfiguration stackAppConfiguration, Environment environment)
            throws Exception {

        log.info("Final config:\n{}", environment.getObjectMapper().writeValueAsString(stackAppConfiguration));
    }
}
