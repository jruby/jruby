# JRuby+Truffle - a High-Performance Implementation of Ruby using Truffle and Graal

The Truffle runtime of JRuby is an experimental implementation of an interpreter
for JRuby using the Truffle AST interpreting framework and the Graal compiler.
It’s an alternative to the IR interpreter and bytecode compiler. The goal is to
be significantly faster, simpler and to have more functionality than other
implementations of Ruby.

JRuby+Truffle is a project of [Oracle Labs](https://labs.oracle.com) and
academic collaborators at the [Institut für Systemsoftware at Johannes Kepler
University Linz](http://ssw.jku.at).

## Authors

* Chris Seaton
* Benoit Daloze
* Kevin Menard
* Petr Chalupa
* Brandon Fish
* Thomas Würthinger
* Matthias Grimmer
* Josef Haider
* Fabio Niephaus
* Matthias Springer
* Lucas Allan Amorim
* Aditya Bhardwaj

And others.

The best way to get in touch with us is to join us in `#jruby` on Freenode, but 
you can also Tweet to @chrisgseaton, or email chris.seaton@oracle.com.

## User Documentation

The [JRuby wiki](https://github.com/jruby/jruby/wiki/Truffle) includes general
user documentation for JRuby+Truffle. This file is documentation for working on
the development of JRuby+Truffle.

## Developer Documentation

Normally you want to use the `truffle-head` branch. We merge to `master` once
with every release of GraalVM, so that it is stable when the JRuby classic
team make their releases.

### Requirements

You will need:

* Java 8 (not 9 EA)
* Ruby 2

### Developer tool

We use a Ruby script to run most commands.

```
$ ruby tool/jt.rb --help
```

Most of us create a symlink to this executable somewhere on our `$PATH` so
that we can simply run.

```
$ jt --help
```

### Building

```
$ jt build
```

### Testing

We have 'specs' which come from the Ruby Spec Suite. These are usually high
quality, small tests, and are our priority at the moment. We also have MRI's
unit tests, which are often very complex and we aren't actively working on now.
Finally, we have tests of our own. The integration tests test more macro use of
Ruby. The ecosystem tests test commands related to Ruby. The gems tests test a
small number of key Ruby 3rd party modules.

The basic test to run every time you make changes is a subset of specs which
runs in reasonable time.

```
$ jt test fast
```

You may also want to regularly run the integration tests.

```
$ jt test integration
```

Other tests can be hard to set up and can require other repositories, so we
don't normally run them locally unless we're working on that functionality.

### Running

`jt ruby` runs JRuby+Truffle. You can use it exactly as you'd run the MRI `ruby`
command. Although it does set a couple of extra options to help you when
developing, such as loading the core lirbary from disk rather than the JAR. `jt
ruby` prints the real command it's running as it starts.

```
$ ruby ...
$ jt ruby ...
```

Note that running Ruby without any arguments does not start a shell. You should
run `jt irb` if you want an interactive shell, or `irb` to run the shell of your
system Ruby or a GraalVM tarball.

### Options

Specify JVM options with `-J-option`.

```
$ jt ruby -J-Xmx1G test.rb
```

JRuby+Truffle options are set with `-Xtruffle...=...`. For example
`-Xtruffle.exceptions.print_java=true` to print Java exceptions before
translating them to Ruby exceptions.

To see all options run `jt ruby -Xtruffle...` (literally, with the three dots).

You can also set JVM options in the `JAVA_OPTS` environment variable (don't
prefix with `-J`), or the `JRUBY_OPTS` variable (do prefix with `-J`). Ruby
command line options and arguments can also be set in `JRUBY_OPTS` or `RUBYOPT`
if they aren't JRuby-specific.

### Running with Graal

To run with a GraalVM binary tarball, set the `GRAALVM_BIN` environment variable
and run with the `--graal` option.

```
$ export GRAALVM_BIN=.../graalvm-0.18-re/bin/java
$ jt ruby --graal ...
```

You can check this is working by printing the value of `Truffle::Graal.graal?`.

```
$ export GRAALVM_BIN=.../graalvm-0.18-re/bin/java
$ jt ruby --graal -e 'p Truffle::Graal.graal?'
```

To run with Graal built from source, set `GRAAL_HOME`.

```
$ export GRAAL_HOME=.../graal-core
$ jt ruby --graal ...
```

Set Graal options as any other JVM option.

```
$ jt ruby --graal -J-Dgraal.TraceTruffleCompilation=true ...
```

We have flags in `jt` to set some options, such as `--trace` for
`-J-Dgraal.TraceTruffleCompilation=true` and `--igv` for
`-J-Dgraal.Dump=Truffle`.

### Testing with Graal

The basic test for Graal is to run our compiler tests. This includes tests that
things partially evaluate as we expect, that things optimise as we'd expect,
that on-stack-replacement works and so on.

```
$ jt test compiler
```

### Benchmarking with Graal

Checkout the `all-ruby-benchmarks` and `benchmark-interface` repositories above
your checkout of JRuby. We usually run like this.

```
$ jt benchmark .../all-ruby-benchmarks/classic/mandelbrot.rb --simple
```

Output is iterations per second, printed roughly every second (more frequently
for the first few iterations).

THe best way to set JVM options here is to use `JAVA_OPTS`.

### Benchmarking without Graal

You can turn off Graal if you want using `--no-graal`.

```
$ jt benchmark --no-graal .../all-ruby-benchmarks/classic/mandelbrot.rb --simple
```

You can benchmark JRuby Classic using `-Xclassic` in `JRUBY_OPTS`.

```
$ JRUBY_OPTS=-Xclassic jt benchmark .../all-ruby-benchmarks/classic/mandelbrot.rb --simple
```

You can benchmark an entirely different implementation using the
`JT_BENCHMARK_RUBY` environment variable.

```
$ JT_BENCHMARK_RUBY=ruby jt benchmark .../all-ruby-benchmarks/classic/mandelbrot.rb --simple
```

### Sulong and C extensions

JRuby runs C extension using Sulong. You should build Sulong from source.

https://github.com/graalvm/sulong

Then set `SULONG_HOME` and `GRAAL_HOME` environment variables to the Sulong
repository.

```
$ export SULONG_HOME=.../sulong
$ export GRAAL_HOME=$SULONG_HOME
```

You can now build the C extension support. Building the OpenSSL C extension is
incomplete, so most people probably want to disable that.

```
$ jt build cexts --no-openssl
```

Get the `jruby-truffle-gem-test-pack` repository.

https://github.com/jruby/jruby-truffle-gem-test-pack

You can then test C extension support.

```
$ export GEM_HOME=../jruby-truffle-gem-test-pack/gems
$ jt test cexts --no-libxml --no-openssl
```

If you want to test `libxml`, remove that flag and set either `LIBXML_HOME` or
`LIBXML_INCLUDE` and `LIBXML_LIB`. Try the same with `OPENSSL_` if you are
adventurous.

To run C extension bechmarks, you first need to compile them.

```
$ jt cextc .../all-ruby-benchmarks/chunky_png/oily_png/
```

Then follow the instructions for benchmarking above, and then try:

```
$  USE_CEXTS=true JRUBY_OPTS=-Xtruffle.cexts.log.load=true jt benchmark .../all-ruby-benchmarks/chunky_png/chunky-color-r.rb --simple
```

These benchmarks have Ruby fallbacks, so we should carefully check that the
C extension is actually being used by looking for these log lines.

```
[ruby] INFO loading cext module ...
```

### mx and integrating with other Graal projects

JRuby can also be built and run using `mx`, like the other Graal projects. This
is intended for special cases such as integrating with other Graal projects, and
we wouldn't recommend using it for normal development. If you do use it, you
should clean before using `jt` again as having built it with `mx` will change
some behaviour.

###  IDEs

The majority of us use IntelliJ IDEA, but it's also [possible to use
Eclipse](https://github.com/jruby/jruby/wiki/Using-Eclipse-with-JRuby-Truffle).
