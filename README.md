# Dropwizard Configstack

This is the open-source version of some code I wrote to make
configuration with Dropwizard more manageable. What I missed the most
was, in no particular order:

* A base, shared config with overrides and/or mix-ins for various
  purposes and environments - stackable configs!
* Variable substitutions like `${this}`, which can reference other
  parts of the config as well, like
  `${/server/applicationConnectors/0/type}`.  Give me one reason we
  shouldn't have that.
* Resources that are loadable from files as usual, but with
  fallbacks to classpath resources.

Dropwizard's config support already does much of this - except stacks -
but not in ways that combine easily.

## Simple usage

This is a Dropwizard bundle, so you know how to use it already.

It has a builder object – in this case a bundler, of course:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(MyConfiguration.class)
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```


### Resolution

This is a default configuration that figures out what you want to load
by looking the arguments to the wizard's `server` command, e.g. suppose
you say :

```
server misc,debug
```

This will load the following resources from classpath or the file
system, assuming they exist, and stack them:

```
MyConfiguration.json
MyConfiguration-misc.json
MyConfiguration-debug.json
```

YAML resources are checked too, so if, say, the following resources
exist, they will be found and loaded too:

```
MyConfiguration.json
MyConfiguration-misc.yaml
MyConfiguration-debug.yaml
```

Finally, if the paths do exist as file names, they will be loaded in
preference to whatever is on the classpath. In general, we
check the bootstrap's existing config source provider for any data
first, then proceed to check the classpath.

### Replacements

If any values are ``${subsitutable}``, they will be looked-up and
substituted with values from the following sources:

1. System properties
1. Environment properties
1. The config itself (using JSON pointer syntax)

## Advanced usage

### Common base config

The bundler takes a common-config argument, which will be loaded
first of all. This is a nice way to enforce some core settings across
many applications:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .addCommonConfig( "common-config")
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

Now our simple example will include:

```
common-config.yaml
MyConfiguration.json
MyConfiguration-misc.json
MyConfiguration-debug.json
```

### Progress logging

Logging the goings-on is a bit tricky. We are, after all,
messing around with configuration, and that includes the log
configuration, so the system doesn't know how to log yet.
The default behavior is to write to System.out,
but you can override that:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .addCommonConfig( "common-config")
        .setProgressLogger(string ->
            System.out.println("### Just testing: " + string))
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

### Writing your own resolver

If you're unhappy with the naming conventions, you can always
write your own ```ConfigResolver```, which can map the string of
names to any resources you like.

```
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .setResolver(new MyVeryOwnConfigResolver())
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

### Writing your own replacer

If you're unhappy with the default look-up/replace-behavior, why
don't you write your own replacer:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .enableClasspathResources()
        .setReplacer(new MyVeryOwnReplacer())
        .bundle());
```
