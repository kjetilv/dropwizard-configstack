package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultStringSubstitutorTest {

    @Test
    public void testJsonReferences() {
        assertThat(
                substitutor().substitute("We just found ${strike}!"),
                is("We just found oil!"));
    }

    @Test
    public void testSimpleReferences() {
        assertThat(
                substitutor().substitute("This is foo ${foo}"),
                is("This is foo bar"));
    }


    @Test
    public void testNestedReferences() {
        assertThat(
                substitutor().substitute("This is foo ${zot}"),
                is("This is foo barbarryar"));
    }

    @Test
    public void testNestedReferencesSomeMore() {
        assertThat(
                substitutor().substitute("This is foo ${foo3}"),
                is("This is foo barbarryarbarbarryar"));
    }

    private StringSubstitutor substitutor() {
        Properties p = new Properties();
        p.setProperty("foo", "bar");
        p.setProperty("strike", "${/sub/sea}");
        p.setProperty("zot", "${foo}${foo2}");

        Map<String, String> env = new HashMap<>();
        env.put("foo2", "barryar");
        env.put("foo3", "${zot}${zot}");

        ObjectNode node = JsonUtils.objectNode();
        node.set("sub", JsonUtils.objectNode().set("sea", JsonUtils.textNode("oil")));

        return new DefaultStringSubstitutor(p, env, node);
    }

}
