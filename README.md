# JRuby - an implementation of the Ruby language on the JVM

Master: [![Build Status](https://travis-ci.org/jruby/jruby.png?branch=master)](https://travis-ci.org/jruby/jruby) 
1.7 branch: [![Build Status](https://travis-ci.org/jruby/jruby.png?branch=jruby-1_7)](https://travis-ci.org/jruby/jruby/branches)

## About

JRuby is an implementation of the [Ruby language](http://www.ruby-lang.org)
using the JVM.

It aims to be a complete, correct and fast implementation of Ruby, at the same
time as providing powerful new features such as concurrency without a
[global-interpreter-lock](http://en.wikipedia.org/wiki/Global_Interpreter_Lock),
true parallelism, and tight integration to the Java language to allow you to
uses Java classes in your Ruby program and to allow JRuby to be embedded into a
Java application.

You can use JRuby simply as a faster version of Ruby, you can use it to run Ruby
on the JVM and access powerful JVM libraries such as highly tuned concurrency
primitives, you can use it to embed Ruby as a scripting language in your Java
program, or many other possibilites.

We're a welcoming community - you can talk to us on [#jruby on Freenode](http://richard.esplins.org/siwi/2011/07/08/getting-started-freenode-irc/).
There are core team members in the EU and US time zones.

Visit the [JRuby website](http://jruby.org) and the [JRuby wiki](https://github.com/jruby/jruby/wiki)
for more information.

## Getting JRuby

To run JRuby you will need a JRE (the JVM runtime environment) version 7 or higher.

Your operating system may provide a JRE and JRuby in a package manager, but you may find that this
version is very old.

An alternative is to use one of the Ruby version managers.

For [`rbenv`](https://github.com/sstephenson/rbenv) you will need the
[`ruby-build`](https://github.com/sstephenson/ruby-build) plugin. You may find that your system
package manager can provide these. Then you can run:

```
$ rbenv install rbenv install jruby-9.0.0.0-dev
```

For [`rvm`](https://rvm.io) you can simply do:

```
$ rvm install jruby
```

You can also [download packages from the JRuby website](http://jruby.org/download) that
you can unpack and run in place.

## Building JRuby from source

See [BUILDING](BUILDING.md) for information about prerequisites, how to compile JRuby from source
and how to test it.

## Authors

Stefan Matthias Aust, Anders Bengtsson, Geert Bevin, Ola Bini,
 Piergiuliano Bossi, Johannes Brodwall, Rocky Burt, Paul Butcher,
 Benoit Cerrina, Wyss Clemens, David Corbin, Benoit Daloze, Thomas E Enebo,
 Robert Feldt, Chad Fowler, Russ Freeman, Joey Gibson, Kiel Hodges,
 Xandy Johnson, Kelvin Liu, Kevin Menard, Alan Moore, Akinori Musha,
 Charles Nutter, Takashi Okamoto, Jan Arne Petersen, Tobias Reif, David Saff,
 Subramanya Sastry, Chris Seaton, Nick Sieger, Ed Sinjiashvili, Vladimir Sizikov,
 Daiki Ueno, Matthias Veit, Jason Voegele, Sergey Yevtushenko, Robert Yokota,
   and many gracious contributors from the community.

JRuby uses code generously shared by the creator of the Ruby language, 
Yukihiro Matsumoto <matz@netlab.co.jp>.

Project Contact: Thomas E Enebo <tom.enebo@gmail.com>

## License

JRuby is licensed to you under three licenses - the EPL 1.0, GPL 2 and LGPL 2.1.
Some components have other licenses and copyright. See the [COPYING](COPYING)
file for more specifics.
