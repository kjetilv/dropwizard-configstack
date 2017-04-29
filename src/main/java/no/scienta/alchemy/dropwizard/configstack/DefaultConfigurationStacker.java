package no.scienta.alchemy.dropwizard.configstack;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultConfigurationStacker implements ConfigurationStacker {

    @Override
    public Collection<String> parse(String serverCommand) {
        return Arrays.stream(serverCommand.split("[^.a-zA-Z_0-9\\-]+"))
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
    }
}
