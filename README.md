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

The stacking is the important part for me.  It needs to happen before
it gets parsed, ie. dumb node-for-node building of a combined JSON AST,
so we're cleanly decoupled from any parsing logic taking place later.

Dropwizard's config support already does much of this - except stacks -
but not in ways that combine easily.

Is this an opinionated bundle? Well, yes, it might have opinions on
how to organize your config. But not that many, I would argue, and I
don't think it holds any extreme views. Hopefully, most people should
be able to reason with it.

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

The stack is popped and applied, so "later" stack elements (further
down) override earlier ones (further up). In this case, the misc
config overrides the base config and the debug config has the last
say.

YAML resources are checked too, so if, say, the following resources
exist, they will be found and loaded too:

```
MyConfiguration.json
MyConfiguration-misc.yaml
MyConfiguration-debug.yaml
```

Finally, if the paths *do* exist as file names, they *will* be loaded in
preference to whatever is on the classpath. This is the default behavior
and we want to preserve that. In general, we check the bootstrap's
existing config source provider for any data first. Only if it fails or
returns null, we proceed to check the classpath.

### Replacements

If any values are ``${subsitutable}``, they will be looked-up and
substituted with values from the following sources:

1. System properties
1. Environment properties
1. The config itself (using JSON pointer syntax)

The first hit applies, so system properties take effect over environment
variables, which in turn take effect over the config.

### What's going on with my config?!

Sometimes the world is not exactly what you expected.  Sometimes, you
even notice the difference.  Sometimes, you need to find out what the
world actually *is* like!  To help out, we log progress about what's
going on – notably, the resources found, their URI's (and a byte count),
as well as the resulting JSON itself.  By default, all this ends up on
standard out. After all, logging isn't configured yet!

Of course, this behavior can be changed, as further reading will reveal.

## Advanced usage

### Common base config

The bundler takes a common-config argument, which will be loaded
first of all. This is a nice way to enforce some core settings across
many applications – consistent logging springs to mind:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .addCommonConfig("common-config")
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
configuration, so the system doesn't know how to log yet!
The default behavior is to write to stdout, but you can override that:

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
write your own ```ConfigResolver```, which can map names to any
resources you like.

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
