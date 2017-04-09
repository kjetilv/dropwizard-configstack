package no.scienta.alchemy.dropwizard.configstack;

import io.dropwizard.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class ConfigStackBundler<C extends Configuration> {

    private final Class<C> configurationType;

    private ConfigResolver<C> resolver;

    private final List<String> commonConfigs = new ArrayList<>();

    private boolean classpathResources;

    private boolean variableReplacements;

    private JsonCombiner jsonCombiner;

    private ProgressLogger progressLogger;

    private JsonReplacer.Replacer replacer;

    public ConfigStackBundler(Class<C> configurationType) {
        this.configurationType = configurationType;
    }

    public ConfigStackBundler<C> setResolver(ConfigResolver<C> resolver) {
        this.resolver = resolver;
        return this;
    }

    public ConfigStackBundler<C> addCommonConfig(String... commonConfigs) {
        this.commonConfigs.addAll(Arrays.asList(commonConfigs));
        return this;
    }

    public ConfigStackBundler<C> enableClasspathResources() {
        this.classpathResources = true;
        return this;
    }

    public ConfigStackBundler<C> enableVariableReplacements() {
        this.variableReplacements = true;
        return this;
    }

    public ConfigStackBundler<C> setReplacer(JsonReplacer.Replacer replacer) {
        if (replacer != null) {
            enableVariableReplacements();
        }
        this.replacer = replacer;
        return this;
    }

    public void setJsonCombiner(JsonCombiner jsonCombiner) {
        this.jsonCombiner = jsonCombiner;
    }

    public ConfigStackBundler<C> setProgressLogger(ProgressLogger progressLogger) {
        this.progressLogger = progressLogger;
        return this;
    }

    public ConfigStackBundle<C> bundle() {
        if (resolver != null) {
            return new ConfigStackBundle<>(
                    resolver,
                    progressLogger,
                    jsonCombiner,
                    classpathResources,
                    variableReplacements,
                    replacer);
        }
        String[] commonConfigs = this.commonConfigs.stream().toArray(String[]::new);
        return new ConfigStackBundle<>(
                configurationType,
                classpathResources,
                variableReplacements,
                replacer,
                progressLogger,
                jsonCombiner,
                commonConfigs);
    }
}
