# A RubySpec fork for CRuby

[![Build Status](https://travis-ci.org/ruby/rubyspec.png)](https://travis-ci.org/ruby/rubyspec)

## FAQ

### Why fork?

Because RubySpec often include [tests which fails on CRuby](http://rubyci.blob.core.windows.net/centos5-64/ruby-trunk/log/20130220T070302Z.diff.html.gz) even if RubySpec is a test suite which verifies whether an implementation is compatible with CRuby or not.
Moreover recent mspec can't ignore specs guarded with ruby\_bug. It breaks running RubySpec because those guards are used to avoid specs which hang or crash.

### Do you receive pull requests?

Yes. If the pull request won't break anything, it will be merged.

## Original Readme

RubySpec is an executable specification for the Ruby programming language. The
specs describe Ruby language syntax as well as the core and standard library
classes. See http://rubyspec.org for more information.

The RubySpec files are written using RSpec-compatible syntax. MSpec is a
purpose-built framework for running RubySpec. For more information, see the
http://github.com/rubyspec/mspec project.

[![Build Status](https://travis-ci.org/rubyspec/rubyspec.png)](https://travis-ci.org/rubyspec/rubyspec)

1. Installing MSpec

The easiest way to run the RubySpecs suite is to install the MSpec gem.

    $ [sudo] gem install mspec

Once the gem is installed, the 'mspec' executable will be available and all
the commands shown below should run.

However, RubySpec often utilizes the latest MSpec features, so you may want to
use MSpec directly from the Git repository.

    $ cd /somewhere
    $ git clone git://github.com/rubyspec/mspec.git

MSpec is now available in '/somewhere/mspec'.

To make the MSpec scripts available, add the MSpec 'bin' directory to you
PATH:

    $ export PATH=/somewhere/mspec/bin:$PATH

Once you have MSpec installed, clone the RubySpec Git repository to run the
specs.

    $ cd /somewhere
    $ git clone git://github.com/rubyspec/rubyspec.git

To run the RubySpec suite:

    $ cd /somewhere/rubyspec
    $ mspec

This will execute all the RubySpec specs using the executable named 'ruby' on
your current PATH.


2. Running Specs with a Specific Ruby Interpreter

Use the '-t' option to specify the Ruby implementation with which to run the
specs. The argument may be a full path to the Ruby binary. For example, to run
RubySpec against '/opt/ruby-enterprise/bin/ruby':

    $ mspec -t /opt/ruby-enterprise/bin/ruby

There are some arguments that are abbreviations for known Ruby implementations.
For example, if you specify 'j', then MSpec will look for 'jruby' in PATH and
run RubySpec against that:

    $ mspec -t j

See 'mspec --help' for a list of '-t' abbreviations.


3. Running Selected Specs

To run a single spec file, pass the filename to 'mspec':

    $ mspec core/kernel/kind_of_spec.rb

You can also pass a directory, in which case all specs in that directories
will be run:

    $ mspec core/kernel

Note however that passing a directory to MSpec may not always be a good idea,
because some specs are language version specific. While there are version
guards in the specs for version-specific behaviors, some classes and libraries
are only for one Ruby version.

RubySpec provides configuration files that include or exclude some spec
directories based on language version. MSpec provides an option to run these
sets of specs. The sets are divided by the natural divisions in RubySpec.

The following command will run all core library specs specific to the language
version:

    $ mspec :core

In similar fashion, the following commands run the respective specs:

    $ mspec :library
    $ mspec :language
