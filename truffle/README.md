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
* Petr Chalupa
* Thomas Würthinger
* Matthias Grimmer
* Josef Haider
* Fabio Niephaus
* Matthias Springer
* Brandon Fish
* Lucas Allan Amorim
* Aditya Bhardwaj

The best way to get in touch with us is to join us in `#jruby` on Freenode, but 
you can also Tweet to @chrisgseaton, @nirvdrum, @eregontp or @pitr-ch, or email
chris.seaton@oracle.com.

## Using Truffle

To run JRuby in Truffle mode, pass the `-X+T` option.

JRuby+Truffle is designed to be run with a JVM that has the Graal compiler. The
easiest way to get this is via the GraalVM, available from the Oracle
Technology Network.

https://github.com/jruby/jruby/wiki/Downloading-GraalVM

But you can also build it yourself, which you will need to do if you are on the
`truffle-head` branch.

https://github.com/jruby/jruby/wiki/Building-Graal

You then need to set the `JAVACMD` environment variable as described on those
pages, and tell the JVM to use the Graal compiler.

```
$ JAVACMD=... bin/jruby -X+T -J-Djvmci.Compiler=graal ...
```

### What to expect

JRuby+Truffle is a research project and is not yet a finished product. Arbitrary
programs are very unlikely to run due to missing functionality, and if they do
run they are unlikely to run fast yet due to requiring new functionality to be
tuned. We are at least a year away from being able to run significant programs
without needing new methods to be implemented.

Windows is currently not supported.

### How we benchmark

We use the [bench9000](https://github.com/jruby/bench9000) benchmarking tool.
This includes classic synthetic benchmarks such as mandelbrot, n-body and
fannkuch, and also kernels from two real-word Ruby gems,
[chunky_png](https://github.com/wvanbergen/chunky_png) and
[psd.rb](https://github.com/layervault/psd.rb).

## Research

* [Chris Seaton's blog posts](http://www.chrisseaton.com/rubytruffle/)
* C. Seaton. **[Specialising Dynamic Techniques for Implementing the Ruby Programming Language](http://chrisseaton.com/phd/)**. PhD thesis, University of Manchester, 2015.
* M. Grimmer, C. Seaton, R. Schatz, T. Würthinger, H. Mössenböck. **[High-Performance Cross-Language Interoperability in a Multi-Language Runtime](http://chrisseaton.com/rubytruffle/dls15-interop/dls15-interop.pdf)**. In Proceedings of 11th Dynamic Languages Symposium (DLS).
* F. Niephaus, M. Springer, T. Felgentreff, T. Pape, R. Hirschfeld. **[Call-target-specific Method Arguments](https://github.com/HPI-SWA-Lab/TargetSpecific-ICOOOLPS/raw/gh-pages/call_target_specific_method_arguments.pdf)**. In Proceedings of the 10th Implementation, Compilation, Optimization of Object-Oriented Languages, Programs and Systems Workshop (ICOOOLPS), 2015.
* B. Daloze, C. Seaton, D. Bonetta, H. Mössenböck. **[Techniques and Applications for Guest-Language Safepoints](http://chrisseaton.com/rubytruffle/icooolps15-safepoints/safepoints.pdf)**. In Proceedings of the 10th Implementation, Compilation, Optimization of Object-Oriented Languages, Programs and Systems Workshop (ICOOOLPS), 2015.
* M. Grimmer, C. Seaton, T. Würthinger, H. Mössenböck. **[Dynamically Composing Languages in a Modular Way: Supporting C Extensions for Dynamic Languages](http://www.chrisseaton.com/rubytruffle/modularity15/rubyextensions.pdf)**. In Proceedings of the 14th International Conference on Modularity, 2015.
* A. Wöß, C. Wirth, D. Bonetta, C. Seaton, C. Humer, and H. Mössenböck. **[An object storage model for the Truffle language implementation framework](http://www.chrisseaton.com/rubytruffle/pppj14-om/pppj14-om.pdf)**. In Proceedings of the International Conference on Principles and Practices of Programming on the Java Platform (PPPJ), 2014.
* C. Seaton, M. L. Van De Vanter, and M. Haupt. **[Debugging at full speed](http://www.lifl.fr/dyla14/papers/dyla14-3-Debugging_at_Full_Speed.pdf)**. In Proceedings of the 8th Workshop on Dynamic Languages and Applications (DYLA), 2014.

Also see the [Ruby Bibliography](http://rubybib.org), and
[publications specifically on Truffle and Graal](https://wiki.openjdk.java.net/display/Graal/Publications+and+Presentations).

## Truffle-Specific Functionality

### Options

There are runtime configuration options that can be set on the command line with
`-Xtrufle.option=value`. To see a list of these run `-Xtruffle...`.

### Truffle Module

The `Truffle`, `Truffle::Debug` and `Truffle::Interop` modules include
Truffle-specific functionality. They're documented for the current development
version at http://lafo.ssw.uni-linz.ac.at/graalvm/jruby/doc/.

### Debugger

See the documentation of the `Truffle::Debug` module at
http://lafo.ssw.uni-linz.ac.at/graalvm/jruby/doc/. `Truffle::Debug.break` will
enter a shell and allow to introspect the program.

If you don't want to modify the program to include a call to
`Truffle::Debug.break` you can break the main thread externally. Run with the
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
available. You need a standard `ruby` from your system to run the tool.

For example:

* `jt build` builds JRuby and Truffle
* `jt run args...` runs JRuby in Truffle mode
* `jt run --graal args...` runs JRuby in Truffle mode, using Graal
* `jt test fast` runs a subset of Truffle tests
