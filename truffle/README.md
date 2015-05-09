# JRuby+Truffle - a High-Performance Truffle Backend for JRuby

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
* Thomas Würthinger
* Matthias Grimmer
* Josef Haider
* Fabio Niephaus
* Matthias Springer
* Brandon Fish
* Lucas Allan Amorim
* Aditya Bhardwaj

The best way to get in touch with us is to join us in `#jruby` on Freenode, but you can also Tweet to @chrisgseaton, @nirvdrum or @eregontp, or email chris.seaton@oracle.com.

## Using Truffle

To run JRuby in Truffle mode, pass the `-X+T` option.

Truffle is designed to be used on the Graal VM rather than a conventional JVM.
Download a build of Graal:

* http://lafo.ssw.uni-linz.ac.at/graalvm/openjdk-8-graalvm-b132-macosx-x86_64-0.6.tar.gz
* http://lafo.ssw.uni-linz.ac.at/graalvm/openjdk-8-graalvm-b132-linux-x86_64-0.6.tar.gz

You can then run JRuby with GraalVM

    JAVACMD=path/to/graal/java bin/jruby -X+T ...

If you want to develop against the latest version of Graal, you should use the
`truffle-head` branch of JRuby. Follow the
[instructions](https://wiki.openjdk.java.net/display/Graal/Instructions) on the
Graal wiki, then additionally execute `./mx.sh maven-install-truffle`, before
you build JRuby.

### What to expect

JRuby+Truffle is a research project and is not yet a finished product. Arbitrary
programs are very unlikely to run due to missing functionality, and if they do
run they are unlikely to run fast yet due to requiring new functionality to be
tuned. We are at least a year away from being able to run significant programs
without needing new methods to be implemented.

Windows support is more limited than Mac and Linux support.

### How we benchmark

We use the [bench9000](https://github.com/jruby/bench9000) benchmarking tool.
This includes classic synthetic benchmarks such as mandelbrot, n-body and
fannkuch, and also kernels from two real-word Ruby gems,
[chunky_png](https://github.com/wvanbergen/chunky_png) and
[psd.rb](https://github.com/layervault/psd.rb).

## Research

* [Chris Seaton's blog posts](http://www.chrisseaton.com/rubytruffle/)
* M. Grimmer, C. Seaton, T. Würthinger, H. Mössenböck. [Dynamically Composing Languages in a Modular Way: Supporting C Extensions for Dynamic Languages](http://www.chrisseaton.com/rubytruffle/modularity15/rubyextensions.pdf). In Proceedings of the 14th International Conference on Modularity, 2015.
* A. Wöß, C. Wirth, D. Bonetta, C. Seaton, C. Humer, and H. Mössenböck. [An object storage model for the Truffle language implementation framework](http://www.chrisseaton.com/rubytruffle/pppj14-om/pppj14-om.pdf). In Proceedings of the International Conference on Principles and Practices of Programming on the Java Platform (PPPJ), 2014.
* C. Seaton, M. L. Van De Vanter, and M. Haupt. [Debugging at full speed](http://www.lifl.fr/dyla14/papers/dyla14-3-Debugging_at_Full_Speed.pdf). In Proceedings of the 8th Workshop on Dynamic Languages and Applications (DYLA), 2014.

Also see the [Ruby Bibliography](http://rubybib.org), and
[publications specifically on Truffle and Graal](https://wiki.openjdk.java.net/display/Graal/Publications+and+Presentations).

## Truffle-Specific Functionality

### Options

There are runtime configuration options that can be set on the command line with
`-Xtrufle.option=value`. To see a list of these run `-Xtruffle...`.

### Truffle Module

The `Truffle` and `Truffle::Debug` modules include Truffle-specific
functionality. They're documented for the current development version at
http://lafo.ssw.uni-linz.ac.at/graalvm/jruby/doc/.

### Debugger

See the documentation of the `Truffle::Debug` module at
http://lafo.ssw.uni-linz.ac.at/graalvm/jruby/doc/. `Truffle::Debug.break` will
enter a shell and allow to introspect the program.

If you don't want to modify the program to include a call to
`Truffle::Debug.break` you can break the main thread externally. run with the
instrumentation server enabled, `-Xtruffle.instrumentation_server_port=8080`.
Then you can send a message to the runtime to break at the current location:

    curl http://localhost:8080/break

### Stack Server

To dump the call stacks of a running Ruby program in Truffle, run with the
instrumentation server enabled and the `passalot` option,
`-Xtruffle.instrumentation_server_port=8080 -Xtruffle.passalot=1`. Then you can
dump the current call stack of all threads:

    curl http://localhost:8080/stacks

## Workflow Tool

Truffle is built as part of JRuby, but if you are working on the Truffle code
base you may appreciate the `jt` tool. To use it alias it in your shell
`function jt { ruby tool/jt.rb $@; }`, then run `jt --help` to see the commands
available.

For example:

* `jt build` builds JRuby and Truffle
* `jt run args...` runs JRuby in Truffle mode
* `jt run --graal args...` runs JRuby in Truffle mode, using Graal
* `jt test fast` runs a subset of Truffle tests
