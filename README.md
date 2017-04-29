# Dropwizard Configstack

[![Build Status](https://travis-ci.org/kjetilv/dropwizard-configstack.svg?branch=master)](https://travis-ci.org/kjetilv/dropwizard-configstack)

This is the some code I wrote to make configuring Dropwizard easier.
I like to express operational parameters as configuration if I can, but
they often pile up, so it becomes a strain to maintain separate configs
for different environments.  What I wanted was to maintain the defaults
in one place, and _stack_ (mix-in if you like) overrides on top, to tune
for test, staging, production etc.

I also wanted a few more points:

* Variable substitutions like `${this}` with lookups in system
  properties and environment variables. It should also be able to
  reference other parts of the config, like
  `${/server/applicationConnectors/0/type}`, ie. JSON pointer syntax.
* Resources that are loadable from files as usual, but with
  fallbacks to classpath resources. This way, defaults can be baked
  into jars, and overrides can be put in the working directory.

The stacking needs to happen before the config gets parsed, ie. dumb
node-for-node building of an aggregate JSON document, cleanly decoupled
from the parsing logic taking place later.

Dropwizard's config support already does much of this - except stacks -
but not in ways that combine easily.

Is this an opinionated bundle? Well, yes, it might have opinions on
how to organize your config. But not that many, I would argue, and I
don't think it holds any extreme views. Hopefully, most people should
be able to reason with it and find that it adds convenience to their
existing workflow. If it does take something away from your workflow,
please give me a hint.

## Simple usage

This is a Dropwizard bundle, so you know how to use it already.

It has a builder object – in this case a bundler, of course:

```java
bootstrap.addBundle(
    ConfigStackBundler.create(MyConfiguration.class)
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

A simpler method with these switches as defaults:

```java
bootstrap.addBundle
    (ConfigStackBundler.defaults(MyConfiguration.class).bundle());
```

Or if you're really lazy:

```java
bootstrap.addBundle
    (ConfigStackBundler.defaultBundle(MyConfiguration.class));
```

### Resolution

The default configuration figures out what you want to load
by looking at the arguments to the wizard's `server` command. Suppose
you say:

```
server misc,debug
```

This will look for the the following resources in the classpath or the
working directory, and load what it finds:

```
MyConfiguration.json
MyConfiguration.yaml
MyConfiguration-misc.json
MyConfiguration-misc.yaml
MyConfiguration-debug.json
MyConfiguration-debug.yaml
```

Of course, you will only have a few of these lying around, like:

```
MyConfiguration.json
MyConfiguration-debug.yaml
```

The stack is popped and applied, so "later" stack elements (further
down the list) override earlier ones (further up the list). In this
case, the misc config overrides the base config and the debug config
has the last say.

Finally, if the paths *do* exist as file names, they *will* be loaded in
preference to whatever is on the classpath. This is the default behavior
and we want to preserve that.

In general, we check the bootstrap's existing config source provider
for any data first. Only if it fails or returns null, do we proceed
to check the classpath.

### Replacements

If any values are ``${subsitutable}``, they will be looked-up and
substituted with values from the following sources:

1. System properties
1. Environment properties
1. The config itself (using JSON pointer syntax)

The first hit applies, so system properties take effect over environment
variables, which in turn take effect over the config.

### What's going on with my config?!

With more going on, the world will sometimes be not exactly what you
expected.  Sometimes, you will even notice the difference!  Sometimes,
you need to find out what the world actually *is* like!

To help out, we log progress about what's going on – notably, the
resources found, their URI's (and a byte count),
as well as the resulting JSON itself.

However, by default, all this ends up on standard out. After all,
logging isn't configured yet!

Of course, this behavior can be changed, as further reading will reveal.

## Advanced usage

The bundler machinery allows overrides for the following cogs:

 * ```ConfigurationResourceResolver``` resolves how strings like
   ```misc```, ```debug``` etc. are mapped to configuration resources.
   It also provides the base name,
 * ```

### Common base config

The bundler takes a common-config argument, which will be loaded
first of all. This is a nice way to enforce some global settings across
several applications – consistent logging springs to mind:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .addCommonConfig("common-config")
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

Now our simple example will also find:

```
common-config.yaml
MyConfiguration.json
MyConfiguration-misc.json
MyConfiguration-debug.json
```

### Ad-hoc configs

Sometimes you feel like adding a twist, and set up a config file that
just tunes a global parameter:

```
server misc,easter-colors,debug
```

However, since it's global parameter you don't want to maintain a
``MyConfiguration-easter-colors.yaml`` file for all apps!  No problem,
just add a ``easter-colors.yaml`` to the mix:

```
common-config.yaml
MyConfiguration.json
MyConfiguration-misc.json
easter-colors.yaml
MyConfiguration-debug.json
```

### Progress logging

Logging the goings-on is a bit tricky. We are, after all,
messing around with configuration, and that includes the log
configuration, so the system doesn't know how to log yet!
The default behavior is to write to stdout, but you can override that
with a custom implementation of ProgressLogger, which is basically a
``Consumer<Supplier<String>>``:

```java
bootstrap.addBundle(
    new ConfigStackBundler<>(StackAppConfiguration.class)
        .addCommonConfig( "common-config")
        .setProgressLogger(string ->
            System.out.println("### Just testing: " + string.get()))
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
        .setConfigurationResolver(new MyVeryOwnConfigResolver())
        .enableClasspathResources()
        .enableVariableReplacements()
        .bundle());
```

This way, you can change the name of the base config, and decide
on a different way to denote stacked configurations.

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
