package no.scienta.alchemy.dropwizard.configstack;

import no.scienta.alchemy.dropwizard.configstack.testapp.StackAppConfiguration;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.scienta.alchemy.dropwizard.configstack.Suffix.JSON;
import static no.scienta.alchemy.dropwizard.configstack.Suffix.YAML;
import static org.junit.Assert.assertThat;

public class DefaultConfigurationLoaderTest {

    private final Random random = new SecureRandom();

    private final List<String> progress = new ArrayList<>();

    @After
    public void clear() {
        progress.clear();
    }

    @Test
    public void testBaseOnly() {
        Collection<LoadedData> loadable = resolver(
                base(JSON)
        ).load("");
        assertThat(loadable, is(base(JSON)));
    }

    @Test
    public void testBaseOnlyYaml() {
        Collection<LoadedData> loadable = resolver(
                base(YAML)
        ).load("");
        assertThat(loadable, is(base(YAML)));
    }

    @Test
    public void testBaseAndStacked() {
        Collection<LoadedData> loadables = resolver(
                base(JSON),
                stacked("debug", JSON)
        ).load("debug");
        assertThat(loadables, are(
                base(JSON),
                stacked("debug", JSON)));
    }

    @Test
    public void testBaseAndDashingStacks() {
        Collection<LoadedData> loadables = resolver(
                base(JSON),
                stacked("debug-dev", JSON)
        ).load("debug-dev");
        assertThat(loadables, are(
                base(JSON),
                stacked("debug-dev", JSON)));
    }

    @Test
    public void testBaseAndStackedMixedFormats() {
        Collection<LoadedData> loadables = resolver(
                base(YAML),
                stacked("debug", JSON)
        ).load("debug");
        assertThat(loadables, are(
                base(YAML),
                stacked("debug", JSON)));
    }

    @Test
    public void testCommonConfigs() {
        ConfigurationLoader resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                stacked("prod", YAML)
        );

        assertThat(resolver.load("prod"), are(
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                stacked("prod", YAML)));

        assertThat(resolver.load(""), are(
                "logging.yaml",
                "serverlogging.json",
                base(JSON)));
    }

    @Test
    public void testSimpleCustoms() {
        ConfigurationLoader resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                stacked("prod", JSON),
                "misc.yaml",
                "foo.json",
                "bar.json"
        );
        assertThat(resolver.load("foo,misc,prod"), are(
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                "foo.json",
                "misc.yaml",
                stacked("prod", JSON)
        ));
    }

    @Test
    public void testIntermingledCustoms() {
        ConfigurationLoader resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                stacked("prod", JSON),
                stacked("prod-cloud", YAML),
                "misc.yaml",
                "foo.json",
                "bar.json"
        );
        assertThat(resolver.load("foo,prod,bar,prod-cloud"), are(
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                "foo.json",
                stacked("prod", JSON),
                "bar.json",
                stacked("prod-cloud", YAML)
        ));
    }

    @Test
    public void testIntermingledCustomsWithSuffixes() {
        ConfigurationLoader resolver = resolver(
                commonConfigs("logging", "serverlogging", "notfound"),
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                stacked("prod", JSON),
                "misc.yaml",
                "foo.json",
                "bar.json"
        );
        assertThat(resolver.load("foo.json,prod,misc.json,notfound.yaml,bar.json"), are(
                "logging.yaml",
                "serverlogging.json",
                base(JSON),
                "foo.json",
                stacked("prod", JSON),
                "bar.json"
        ));
    }

    private String[] commonConfigs(String... paths) {
        return paths;
    }

    private ConfigurationLoader resolver(String... paths) {
        return resolver(new String[0], paths);
    }

    private ConfigurationLoader resolver(String[] commonConfigs, String... paths) {
        return new DefaultConfigurationLoader(
                new MockedConfigurationSourceProvider(paths),
                new BasenameVariationsResourceResolver(StackAppConfiguration.class),
                Arrays.asList(commonConfigs),
                supplier -> progress.add(supplier.get()));
    }

    private Matcher<Iterable<LoadedData>> is(String path) {
        return are(path);
    }

    private Matcher<Iterable<LoadedData>> are(String... paths) {
        List<Matcher<LoadedData>> matchers = Arrays.stream(paths).map(this::path).collect(Collectors.toList());
        return new BaseMatcher<Iterable<LoadedData>>() {
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
                    Matcher<LoadedData> matcher = matchers.get(i);
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

    private Matcher<LoadedData> path(String path) {
        return new BaseMatcher<LoadedData>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof LoadedData && ((LoadedData) o).getPath().equals(path);
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

}
