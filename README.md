# JRuby -  A Java implementation of the Ruby language

[![Build Status](https://travis-ci.org/jruby/jruby.png?branch=jruby-1_7)](https://travis-ci.org/jruby/jruby/branches)

Authors: Stefan Matthias Aust, Anders Bengtsson, Geert Bevin,
 Piergiuliano Bossi, Johannes Brodwall, Rocky Burt, Paul Butcher,
 Benoit Cerrina, Wyss Clemens, David Corbin, Thomas E Enebo, Robert Feldt,
 Russ Freeman, Chad Fowler, Joey Gibson, Kiel Hodges, Xandy Johnson,
 Kelvin Liu, Alan Moore, Akinori Musha, Charles Nutter, Takashi Okamoto
 Jan Arne Petersen, Tobias Reif, David Saff, Ed Sinjiashvili, Daiki Ueno
 Matthias Veit, Jason Voegele, Sergey Yevtushenko, Robert Yokota, 
 Ola Bini, Nick Sieger, Vladimir Sizikov, and many gracious contributors
 from the community.

Project Contact: Thomas E Enebo <enebo@acm.org>

JRuby also uses code generously shared by the creator of the Ruby language, 
Yukihiro Matsumoto <matz@netlab.co.jp>.

## About

JRuby is the effort to recreate the Ruby (http://www.ruby-lang.org) interpreter
in Java.

The Java version is tightly integrated with Java to allow both to script
any Java class and to embed the interpreter into any Java application. 
See the [docs](docs) directory for more information.

## Prerequisites

JRuby 1.7.x requires Java 6 or greater.

## Run

    bin/jruby rubyfile.rb

interprets the file `rubyfile.rb`.

If you checked out from the repository or downloaded the source distribution,
see the next section to build JRuby first.

## Compiling from source

See [BUILDING](BUILDING.md) for more information.

## Testing

In order to run the unit tests, copy the `build_lib/junit.jar` file to either
`$ANT_HOME/lib/junit.jar` or `~/.ant/lib/junit.jar`.

See [README.test](docs/README.test.md) for more information.

## More Information

Visit http://jruby.org for more information.

Visit http://jruby.github.io/jruby for the Maven Site documentation.

## License

Read the [COPYING](COPYING) file.
