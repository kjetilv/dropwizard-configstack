package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.net.URI;

@SuppressWarnings({"WeakerAccess", "unused"})
public class StackAppConfiguration extends Configuration {

    public Boolean flag;

    public int size;

    public SubConfiguration sub;

    public static class SubConfiguration {

        public String mode;

        public URI remoteURI;

        public int bar;
    }
}
