package no.scienta.alchemy.dropwizard.configstack.app;

import java.net.URI;

@SuppressWarnings({"WeakerAccess", "unused"})
public class StackAppConfiguration extends BaseConfiguration {

    public Boolean flag;

    public int size;

    public SubConfiguration sub;

    public String stuff;

    public String[] strings;

    public static class SubConfiguration {

        public String mode;

        public URI remoteURI;

        public int bar;
    }
}
