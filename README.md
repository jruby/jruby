# JRuby -  An implementation of the Ruby language on the JVM

Master: [![Build Status](https://travis-ci.org/jruby/jruby.png?branch=master)](https://travis-ci.org/jruby/jruby) 
1.7 branch: [![Build Status](https://travis-ci.org/jruby/jruby.png?branch=jruby-1_7)](https://travis-ci.org/jruby/jruby/branches)

Authors: Stefan Matthias Aust, Anders Bengtsson, Geert Bevin, Ola Bini,
 Piergiuliano Bossi, Johannes Brodwall, Rocky Burt, Paul Butcher,
 Benoit Cerrina, Wyss Clemens, David Corbin, Benoit Daloze, Thomas E Enebo,
 Robert Feldt, Chad Fowler, Russ Freeman, Joey Gibson, Kiel Hodges,
 Xandy Johnson, Kelvin Liu, Kevin Menard, Alan Moore, Akinori Musha,
 Charles Nutter, Takashi Okamoto, Jan Arne Petersen, Tobias Reif, David Saff,
 Subramanya Sastry, Chris Seaton, Nick Sieger, Ed Sinjiashvili, Vladimir Sizikov,
 Daiki Ueno, Matthias Veit, Jason Voegele, Sergey Yevtushenko, Robert Yokota,
   and many gracious contributors from the community.

Project Contact: Thomas E Enebo <enebo@acm.org>

JRuby also uses code generously shared by the creator of the Ruby language, 
Yukihiro Matsumoto <matz@netlab.co.jp>.

## About

JRuby is an effort to implement the [Ruby language](http://www.ruby-lang.org)
on top of the JVM.

JRuby is tightly integrated with the JVM to allow both to script
any Java class and to embed the interpreter into any Java application. 
See the [docs](docs) directory for more information.

## Prerequisites

* A Java 7-compatible (or higher) Java development kit (JDK)
* Maven 3+
* Apache Ant 1.8+ (see https://github.com/jruby/jruby/issues/2236)

## Run

    bin/jruby rubyfile.rb

interprets the file `rubyfile.rb`.

If you checked out from the repository or downloaded the source distribution,
see the next section to build JRuby first.

## Compiling from source

See [BUILDING](BUILDING.md) for more information.

## Testing

See [BUILDING: Developing and Testing](BUILDING.md#developing-and-testing) for
more information.

## More Information

Visit http://jruby.org for more information.

Visit http://jruby.github.io/jruby for the Maven Site documentation.

## License

Read the [COPYING](COPYING) file.
