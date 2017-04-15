package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.scienta.alchemy.dropwizard.configstack.Suffix.JSON;
import static no.scienta.alchemy.dropwizard.configstack.Suffix.YAML;
import static org.junit.Assert.assertThat;

public class LoadablesResolverTest {

    private final Random random = new SecureRandom();

    private List<String> progress = new ArrayList<>();

    @After
    public void clear() {
        progress.clear();
    }

    @Test
    public void testBaseOnly() {
        List<Loadable> loadable = resolver(
                base(JSON)
        ).resolveLoadables("");
        assertThat(loadable, is(base(JSON)));
    }

    @Test
    public void testBaseOnlyYaml() {
        List<Loadable> loadable = resolver(
                base(YAML)
        ).resolveLoadables("");
        assertThat(loadable, is(base(YAML)));
    }

    @Test
    public void testBaseAndStacked() {
        List<Loadable> loadables = resolver(
                base(JSON),
                stacked("debug", JSON)
        ).resolveLoadables("debug");
        assertThat(loadables, are(
                base(JSON),
                stacked("debug", JSON)));
    }

    @Test
    public void testBaseAndDashingStacks() {
        List<Loadable> loadables = resolver(
                base(JSON),
                stacked("debug-dev", JSON)
        ).resolveLoadables("debug-dev");
        assertThat(loadables, are(
                base(JSON),
                stacked("debug-dev", JSON)));
    }

    @Test
    public void testBaseAndStackedMixedFormats() {
        List<Loadable> loadables = resolver(
                base(YAML),
                stacked("debug", JSON)
        ).resolveLoadables("debug");
        assertThat(loadables, are(
                base(YAML),
                stacked("debug", JSON)));
    }

    @Test
    public void testCommonConfigs() {
        LoadablesResolver<StackAppConfiguration> resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                stacked("prod", YAML)
        );

        assertThat(resolver.resolveLoadables("prod"), are(
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                stacked("prod", YAML)));

        assertThat(resolver.resolveLoadables(""), are(
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON)));
    }

    @Test
    public void testSimpleCustoms() {
        LoadablesResolver<StackAppConfiguration> resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                stacked("prod", JSON),
                YAML.suffixed("misc"),
                JSON.suffixed("foo"),
                JSON.suffixed("bar")
        );
        assertThat(resolver.resolveLoadables("foo,misc,prod"), are(
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                JSON.suffixed("foo"),
                YAML.suffixed("misc"),
                stacked("prod", JSON)
        ));
    }

    @Test
    public void testIntermingledCustoms() {
        LoadablesResolver<StackAppConfiguration> resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                stacked("prod", JSON),
                stacked("prod-cloud", YAML),
                YAML.suffixed("misc"),
                JSON.suffixed("foo"),
                JSON.suffixed("bar")
        );
        assertThat(resolver.resolveLoadables("foo,prod,bar,prod-cloud"), are(
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                JSON.suffixed("foo"),
                stacked("prod", JSON),
                JSON.suffixed("bar"),
                stacked("prod-cloud", YAML)
        ));
    }

    @Test
    public void testIntermingledCustomsWithSuffixes() {
        LoadablesResolver<StackAppConfiguration> resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                stacked("prod", JSON),
                YAML.suffixed("misc"),
                JSON.suffixed("foo"),
                JSON.suffixed("bar")
        );
        assertThat(resolver.resolveLoadables("foo.json,prod,misc.json,notfound.yaml,bar.json"), are(
                YAML.suffixed("logging"),
                JSON.suffixed("serverlogging"),
                base(JSON),
                JSON.suffixed("foo"),
                stacked("prod", JSON),
                JSON.suffixed("bar")
        ));
    }

    private String[] commonConfigs(String... paths) {
        return paths;
    }

    private LoadablesResolver<StackAppConfiguration> resolver(String... paths) {
        return resolver(new String[0], paths);
    }

    private LoadablesResolver<StackAppConfiguration> resolver(String[] commonConfigs, String... paths) {
        return new LoadablesResolver<>(
                new MockedConfigurationSourceProvider(paths),
                new BasenameVariationsResolver<>(StackAppConfiguration.class, commonConfigs),
                progress::add);
    }

    private Matcher<Iterable<Loadable>> is(String path) {
        return are(path);
    }

    private Matcher<Iterable<Loadable>> are(String... paths) {
        List<Matcher<Loadable>> matchers = Arrays.stream(paths).map(this::path).collect(Collectors.toList());
        return new BaseMatcher<Iterable<Loadable>>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof List<?> && paths.length == ((List) o).size() &&
                        IntStream.range(0, paths.length).allMatch(i ->
                                matchers.get(i).matches(((List) o).get(i)));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("List of [");
                for (int i = 0; i < matchers.size(); i++) {
                    Matcher<Loadable> matcher = matchers.get(i);
                    description.appendText("\n  ");
                    matcher.describeTo(description);
                    if (i + 1 < matchers.size()) {
                        description.appendText(", ");
                    }
                }
                description.appendText("]");
            }
        };
    }

    private Matcher<Loadable> path(String path) {
        return new BaseMatcher<Loadable>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Loadable && ((Loadable) o).getPath().equals(path);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("loadable with path " + path);
            }
        };
    }

    private String base(Suffix... suffices) {
        return suffix(suffices).suffixed(StackAppConfiguration.class.getSimpleName());
    }

    private String stacked(String element, Suffix... suffices) {
        return suffix(suffices).suffixed(StackAppConfiguration.class.getSimpleName() + "-" + element);
    }

    private Suffix suffix(Suffix... suffices) {
        return suffices.length > 0 ? suffices[0] : random.nextBoolean() ? JSON : YAML;
    }

    private static class MockedConfigurationSourceProvider implements ConfigurationSourceProvider {

        private final String[] paths;

        private MockedConfigurationSourceProvider(String... paths) {
            this.paths = paths;
        }

        @Override
        public InputStream open(String resource) throws IOException {
            return Arrays.stream(paths)
                    .filter(resource::equals)
                    .map(res -> {
                        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
                        return stream == null ? new ByteArrayInputStream("{}".getBytes()) : stream;
                    })
                    .findAny()
                    .orElseThrow(() ->
                            new IllegalStateException("No resource " + resource + " found"));
        }
    }
}
