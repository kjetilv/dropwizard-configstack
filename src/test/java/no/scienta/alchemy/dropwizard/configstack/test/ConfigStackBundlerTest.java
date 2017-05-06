package no.scienta.alchemy.dropwizard.configstack.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.dropwizard.Bundle;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import no.scienta.alchemy.dropwizard.configstack.*;
import no.scienta.alchemy.dropwizard.configstack.app.StackApp;
import no.scienta.alchemy.dropwizard.configstack.app.StackAppConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
    public void testCustomConfigResolver() throws IOException {
        ConfigurationResourceResolver resolver = mock(ConfigurationResourceResolver.class);
        when(resolver.baseResource()).thenReturn(
                Stream.of("foo.json"));
        when(resolver.stackedResource(eq("debug.json"))).thenReturn(
                Stream.of("debug.json"));

        Bundle bundle = base()
                .setConfigurationResourceResolver(resolver)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap =
                mount(bundle,
                        new MockedConfigurationSourceProvider(
                                "foo.json",
                                "debug.json"));
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("debug.json");
        assertNotNull(open);

        verify(resolver, atLeastOnce()).baseResource();
        verify(resolver, atLeastOnce()).stackedResource(eq("debug.json"));

        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetCustomBuilder() throws IOException {
        ConfigurationAssembler combiner = mock(ConfigurationAssembler.class);
        when(combiner.assemble(anyCollection())).thenReturn(JsonNodeFactory.instance.objectNode());

        Bundle bundle = base()
                .setConfigurationBuilder(combiner)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap =
                mount(bundle,
                        new MockedConfigurationSourceProvider("foo.json"));
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("foo.json");
        assertNotNull(open);
        verify(combiner, atLeastOnce()).assemble(anyCollection());
        assertFalse(progress.isEmpty());
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void testSetCustomLoader() throws IOException {
        ConfigurationLoader loader = mock(ConfigurationLoader.class);
        when(loader.load(eq("foo"))).thenReturn
                (Collections.singleton(LoadedData.create("/foo.json", new ByteArrayInputStream("{}".getBytes()))));

        Bundle bundle = base()
                .setConfigurationLoader(loader)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        verify(loader, atLeastOnce()).load(Arrays.asList("foo"));
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetCustomSubstitutor() throws IOException {
        ConfigurationSubstitutor sub = mock(ConfigurationSubstitutor.class);
        when(sub.substitute(any(JsonNode.class))).thenReturn(JsonNodeFactory.instance.objectNode());

        Bundle bundle = base()
                .setConfigurationSubstitutor(sub)
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        verify(sub, atLeastOnce()).substitute(any(JsonNode.class));
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testSetSubstitutor() throws IOException {
        AtomicBoolean called = new AtomicBoolean();
        Bundle bundle = base()
                .setSubstitutor(s -> {
                    called.compareAndSet(false, true);
                    return s + s;
                })
                .bundle();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        assertTrue(called.get());
        assertFalse(progress.isEmpty());
    }

    @Test
    public void testArrayStrategy() throws IOException {
        Bundle bundle = base()
                .setArrayStrategy(ArrayStrategy.REPLACE)
                .bundle();
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, new MockedConfigurationSourceProvider(objectMapper)
                .content(StackAppConfiguration.class.getSimpleName() + ".json",
                        new StackAppConfiguration() {{
                            strings = new String[]{"should", "be", "replaced"};
                        }})
                .content(StackAppConfiguration.class.getSimpleName() + "-debug.json",
                        new StackAppConfiguration() {{
                            strings = new String[]{"winning!"};
                        }}));
        InputStream debug = assertedStackingProvider(bootstrap).open("debug");
        StackAppConfiguration config =
                objectMapper.readerFor(StackAppConfiguration.class).readValue(debug);
        assertThat(config.strings.length, is(1));
        assertThat(config.strings[0], is("winning!"));
    }

    @Test
    public void testQuiet() throws IOException {
        Bundle bundle = base().quiet().bundle();
        assertNoProgress(bundle);
    }

    @Test
    public void testProgressLoggerAsConsumer() throws IOException {
        List<String> localLog = new ArrayList<>();
        Consumer<Supplier<String>> progressLogger = s -> localLog.add(s.get());
        Bundle bundle = base().quiet().setProgressLogger(progressLogger).bundle();
        assertNoProgress(bundle);
        assertFalse(localLog.isEmpty());
    }

    @Test
    public void testProgressLogger() throws IOException {
        List<Supplier<String>> localLog = new ArrayList<>();
        Bundle bundle = base().quiet().setProgressLogger(localLog::add).bundle();
        assertNoProgress(bundle);
        assertFalse(localLog.isEmpty());
    }

    private void assertNoProgress(Bundle bundle) throws IOException {
        Bootstrap<StackAppConfiguration> bootstrap = mount(bundle, null);
        ConfigurationSourceProvider provider = assertedStackingProvider(bootstrap);
        InputStream open = provider.open("foo");
        assertNotNull(open);
        assertTrue(progress.isEmpty());
    }

    private ConfigStackBundler<StackAppConfiguration> base() {
        return ConfigStackBundler
                .defaults(StackAppConfiguration.class)
                .setProgressLogger(string -> progress.add(string.get()));
    }

    private ConfigurationSourceProvider assertedStackingProvider(Bootstrap<StackAppConfiguration> bootstrap) {
        ConfigurationSourceProvider provider =
                bootstrap.getConfigurationSourceProvider();
        assertTrue(provider.getClass().getPackage().getName()
                .startsWith("no.scienta.alchemy"));
        return provider;
    }

    private Bootstrap<StackAppConfiguration> mount(Bundle bundle, ConfigurationSourceProvider provider) {
        StackApp stackApp = new StackApp();
        Bootstrap<StackAppConfiguration> bootstrap = new Bootstrap<>(stackApp);
        if (provider != null) {
            bootstrap.setConfigurationSourceProvider(provider);
        }
        bundle.initialize(bootstrap);
        return bootstrap;
    }
}
