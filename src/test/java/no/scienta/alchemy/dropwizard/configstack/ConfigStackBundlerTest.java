package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackApp;
import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static no.scienta.alchemy.dropwizard.configstack.Suffix.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConfigStackBundlerTest {

    private List<String> progress;

    @Before
    public void start() {
        progress = new ArrayList<>();
    }

    @After
    public void stop() {
        progress = null;
    }

    @Test
    public void testCustomConfigResolver() {
        ConfigurationResourceResolver resolver = mock(ConfigurationResourceResolver.class);
        when(resolver.baseResource()).thenReturn(
                Stream.of("foo.json"));
        when(resolver.stackedResource(eq("debug.json"))).thenReturn(
                Stream.of("debug.json"));

        ConfigStackBundle bundle = base()
                .setConfigurationResolver(resolver)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap =
                mount(bundle,
                        new MockedConfigurationSourceProvider(
                                "foo.json",
                                "debug.json"));
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("debug.json");
        assertNotNull(open);

        verify(resolver, atLeastOnce()).baseResource();
        verify(resolver, atLeastOnce()).stackedResource(eq("debug.json"));

        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetCustomBuilder() {
        ConfigurationBuilder combiner = mock(ConfigurationBuilder.class);
        when(combiner.build(anyCollection())).thenReturn(JsonNodeFactory.instance.objectNode());

        ConfigStackBundle bundle = base()
                .setConfigurationBuilder(combiner)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap =
                mount(bundle,
                        new MockedConfigurationSourceProvider("foo.json"));
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("foo.json");
        assertNotNull(open);
        verify(combiner, atLeastOnce()).build(anyCollection());
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetCustomLoader() {
        ConfigurationLoader loader = mock(ConfigurationLoader.class);
        when(loader.load(eq("foo"))).thenReturn
                (Collections.singleton(LoadedData.create("/foo.json", new ByteArrayInputStream("{}".getBytes()))));

        ConfigStackBundle bundle = base()
                .setConfigurationLoader(loader)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        verify(loader, atLeastOnce()).load(eq("foo"));
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetCustomSubstitutor() {
        ConfigurationSubstitutor sub = mock(ConfigurationSubstitutor.class);
        when(sub.substitute(any(JsonNode.class))).thenReturn(JsonNodeFactory.instance.objectNode());

        ConfigStackBundle bundle = base()
                .setConfigurationSubstitutor(sub)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        verify(sub, atLeastOnce()).substitute(any(JsonNode.class));
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetSubstitutor() {
        AtomicBoolean called = new AtomicBoolean();
        ConfigStackBundle bundle = base()
                .setSubstitutor((Function<String, String>) s -> {
                    called.compareAndSet(false, true);
                    return s + s;
                })
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        assertTrue(called.get());
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testArrayStrategy() throws IOException {
        ConfigStackBundle bundle = base()
                .setArrayStrategy(ArrayStrategy.REPLACE)
                .bundle();
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, new MockedConfigurationSourceProvider(objectMapper)
                .content(JSON.suffixed(StackAppConfiguration.class.getSimpleName()),
                        new StackAppConfiguration() {{
                            strings = new String[]{"should", "be", "replaced"};
                        }})
                .content(JSON.suffixed(StackAppConfiguration.class.getSimpleName() + "-debug"),
                        new StackAppConfiguration() {{
                            strings = new String[]{"winning!"};
                        }}));
        InputStream debug = stackingProvider(bootstrap).open("debug");
        StackAppConfiguration config =
                objectMapper.readerFor(StackAppConfiguration.class).readValue(debug);
        assertThat(config.strings.length, is(1));
        assertThat(config.strings[0], is("winning!"));
    }

    @Test
    public void testQuiet() {
        ConfigStackBundle bundle = base().quiet().bundle();
        assertNoProgress(bundle);
    }

    @Test
    public void testProgressLoggerAsConsumer() {
        Consumer<Supplier<String>> progressLogger = s -> {
        };
        ConfigStackBundle bundle = base().quiet().setProgressLogger(progressLogger).bundle();
        assertNoProgress(bundle);
    }

    @Test
    public void testProgressLogger() {
        ConfigStackBundle bundle = base().quiet().setProgressLogger(s -> {
        }).bundle();
        assertNoProgress(bundle);
    }

    private void assertNoProgress(ConfigStackBundle bundle) {
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        StackingConfigurationSourceProvider provider = stackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        assertTrue(progress.isEmpty());
    }

    private ConfigStackBundler<StackAppConfiguration> base() {
        return ConfigStackBundler
                .defaults(StackAppConfiguration.class)
                .setProgressLogger(string -> progress.add(string.get()));
    }

    private StackingConfigurationSourceProvider stackingProvider(Bootstrap<StackAppConfiguration> bootstrap) {
        ConfigurationSourceProvider provider =
                bootstrap.getConfigurationSourceProvider();
        assertThat(provider, CoreMatchers.instanceOf(StackingConfigurationSourceProvider.class));

        return (StackingConfigurationSourceProvider) provider;
    }

    private Bootstrap<StackAppConfiguration> mount(ConfigStackBundle bundle,
                                                   ConfigurationSourceProvider provider) {
        StackApp stackApp = new StackApp();
        Bootstrap<StackAppConfiguration> bootstrap = new Bootstrap<>(stackApp);
        if (provider != null) {
            bootstrap.setConfigurationSourceProvider(provider);
        }
        bundle.initialize(bootstrap);
        return bootstrap;
    }
}
