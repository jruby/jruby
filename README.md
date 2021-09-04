# JRuby - an implementation of the Ruby language on the JVM

Master: [![Build Status](https://travis-ci.com/jruby/jruby.svg?branch=master)](https://travis-ci.com/jruby/jruby)
9.2 branch: [![Build Status](https://travis-ci.com/jruby/jruby.svg?branch=jruby-9.2)](https://travis-ci.com/jruby/jruby/branches)

## About

JRuby is an implementation of the [Ruby language](http://www.ruby-lang.org)
using the JVM.

It aims to be a complete, correct and fast implementation of Ruby, at the same
time as providing powerful new features such as concurrency without a
[global-interpreter-lock](http://en.wikipedia.org/wiki/Global_Interpreter_Lock),
true parallelism, and tight integration to the Java language to allow you to
use Java classes in your Ruby program and to allow JRuby to be embedded into a
Java application.

You can use JRuby simply as a faster version of Ruby, you can use it to run Ruby
on the JVM and access powerful JVM libraries such as highly tuned concurrency
primitives, you can use it to embed Ruby as a scripting language in your Java
program, or many other possibilities.

We're a welcoming community - you can talk to us on [#jruby on Freenode](http://richard.esplins.org/siwi/2011/07/08/getting-started-freenode-irc/).
There are core team members in the EU and US time zones.

Visit the [JRuby website](https://www.jruby.org/) and the [JRuby wiki](https://github.com/jruby/jruby/wiki)
for more information.

## Getting JRuby

To run JRuby you will need a JRE (the Java VM runtime environment) version 8 or higher.

Your operating system may provide a JRE and JRuby in a package manager, but you may find that this
version is very old.

An alternative is to use one of the [Ruby version managers](https://www.ruby-lang.org/en/documentation/installation/#managers).

For [`rbenv`](https://github.com/sstephenson/rbenv) you will need the
[`ruby-build`](https://githubcom/sstephenson/ruby-build) plugin. You may find that your system
package manager can provide these. To see which versions of JRuby are available you should run:

```
$ rbenv install jruby
```

Note: if you do not regularly git update rbenv this list of versions may be out of date.

We recommend always selecting the latest version of JRuby from the list. 
You can install that particular version (9.2.13.0 is just for illustration):


```
$ rbenv install jruby-9.2.13.0
```

For [`rvm`](https://rvm.io) you can simply do:

```
$ rvm install jruby
```

Using [`Homebrew`](https://brew.sh/) works too:

```
$ brew install jruby
```

You can also [download packages from the JRuby website](https://www.jruby.org/download) that
you can unpack and run in place.

## Building JRuby

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

JRuby is licensed under a tri EPL/GPL/LGPL license. You can use it,
redistribute it and/or modify it under the terms of the:

  Eclipse Public License version 2.0
    OR
  GNU General Public License version 2
    OR
  GNU Lesser General Public License version 2.1

Some components have other licenses and copyright. See the [COPYING](COPYING)
file for more specifics.
