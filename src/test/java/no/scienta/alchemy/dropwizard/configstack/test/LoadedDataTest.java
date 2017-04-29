package no.scienta.alchemy.dropwizard.configstack.test;

import no.scienta.alchemy.dropwizard.configstack.LoadedData;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LoadedDataTest {

    @Test
    public void testContent() {
        assertThat(getLoadedData("json").hasContent(), is(true));
    }

    @Test
    public void testPath() {
        assertThat(getLoadedData("json").getPath(), is("foo.json"));
    }

    @Test
    public void testNotYaml() {
        assertThat(getLoadedData("json").isYaml(), is(false));
    }

    @Test
    public void testYaml() {
        assertThat(getLoadedData("yaml").isYaml(), is(true));
    }

    private LoadedData getLoadedData(String suff) {
        Function<InputStream, LoadedData> foo = LoadedData.forPath("foo." + suff);
        return foo.apply(new ByteArrayInputStream("content".getBytes()));
    }
}
