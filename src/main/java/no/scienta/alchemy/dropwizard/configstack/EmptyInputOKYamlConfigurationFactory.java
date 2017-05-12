package no.scienta.alchemy.dropwizard.configstack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;

import javax.validation.Validator;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

class EmptyInputOKYamlConfigurationFactory<T>
        extends YamlConfigurationFactory<T> {

    private final ConfigurationResourceResolver configurationResourceResolver;

    private final ConfigurationSourceProvider provider;

    EmptyInputOKYamlConfigurationFactory(
            Class<T> klass,
            Validator validator,
            String propertyPrefix, ConfigurationResourceResolver configurationResourceResolver, ConfigurationSourceProvider provider, ObjectMapper objectMapper) {
        super(klass, validator, objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES), propertyPrefix);
        this.configurationResourceResolver = configurationResourceResolver;
        this.provider = provider;
    }

    @Override
    public T build() throws IOException, ConfigurationException {
        Collection<String> base =
                configurationResourceResolver.baseResource().collect(Collectors.toList());
        return base.isEmpty()
                ? super.build()
                : super.build(provider, base.iterator().next());
    }
}
