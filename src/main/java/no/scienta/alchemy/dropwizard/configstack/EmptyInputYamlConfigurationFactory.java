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

class EmptyInputYamlConfigurationFactory<T>
        extends YamlConfigurationFactory<T> {

    private final ConfigurationResourceResolver configurationResourceResolver;

    private final ConfigurationSourceProvider provider;

    EmptyInputYamlConfigurationFactory(
            Class<T> klass,
            Validator validator,
            ObjectMapper objectMapper,
            String propertyPrefix,
            ConfigurationResourceResolver configurationResourceResolver,
            ConfigurationSourceProvider provider) {
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
