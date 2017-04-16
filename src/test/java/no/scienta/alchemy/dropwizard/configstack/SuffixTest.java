package no.scienta.alchemy.dropwizard.configstack;

import org.junit.Test;

import static no.scienta.alchemy.dropwizard.configstack.Suffix.JSON;
import static no.scienta.alchemy.dropwizard.configstack.Suffix.YAML;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class SuffixTest {

    @Test
    public void suffix() {
        assertThat(JSON.suffixed("foo"), is("foo.json"));
        assertThat(YAML.suffixed("foo"), is("foo.yaml"));
        assertThat(JSON.suffixed("bar"), not(is("bar.yaml")));
        assertThat(YAML.suffixed("bar"), not(is("bar.json")));
    }

    @Test
    public void suffixed() {
        assertThat(JSON.isSuffixed("foo.json"), is(true));
        assertThat(JSON.isSuffixed("foo.yaml"), not(is(true)));
        assertThat(YAML.isSuffixed("foo.yaml"), is(true));
        assertThat(YAML.isSuffixed("foo.json"), not(is(true)));
    }
}
