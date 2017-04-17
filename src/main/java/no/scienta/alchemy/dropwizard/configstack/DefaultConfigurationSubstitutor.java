package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.JsonNode;

final class DefaultConfigurationSubstitutor implements ConfigurationSubstitutor {

    private final StringSubstitutor substitutor;

    DefaultConfigurationSubstitutor(StringSubstitutor substitutor) {
        this.substitutor = substitutor;
    }

    @Override
    public JsonNode substitute(JsonNode combined) {
        return JsonSubstitutor.substitute(combined, stringSubstitutor(combined));
    }

    private StringSubstitutor stringSubstitutor(JsonNode combined) {
        return this.substitutor == null
                ? new DefaultStringSubstitutor(System.getProperties(), System.getenv(), combined)
                : this.substitutor;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "]";
    }
}
