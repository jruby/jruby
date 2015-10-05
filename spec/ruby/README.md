# RubySpec

[![Build Status](https://travis-ci.org/ruby/rubyspec.svg)](https://travis-ci.org/ruby/rubyspec)

RubySpec is an executable specification for the Ruby programming language.  
The specs describe the Ruby language syntax as well as the core and standard library classes.

The RubySpec files are written using a RSpec-like syntax.
MSpec is the purpose-built framework for running RubySpec.
For more information, see the [MSpec](http://github.com/ruby/mspec) project.

### Running the specs

First, clone this repository:

    $ git clone https://github.com/ruby/rubyspec.git

Then move to it:

    $ cd rubyspec

Clone [MSpec](http://github.com/ruby/mspec):

    $ git clone https://github.com/ruby/mspec.git ../mspec

And run the RubySpec suite:

    $ ../mspec/bin/mspec

This will execute all the RubySpec specs using the executable named `ruby` on your current PATH.

### Running Specs with a Specific Ruby Implementation

Use the `-t` option to specify the Ruby implementation with which to run the specs.  
The argument may be a full path to the Ruby binary.

    $ ../mspec/bin/mspec -t /path/to/some/bin/ruby

### Running Selected Specs

To run a single spec file, pass the filename to `mspec`:

    $ ../mspec/bin/mspec core/kernel/kind_of_spec.rb

You can also pass a directory, in which case all specs in that directories will be run:

    $ ../mspec/bin/mspec core/kernel

Finally, you can also run them per group as defined in `default.mspec`.  
The following command will run all language specs:

    $ ../mspec/bin/mspec :language

In similar fashion, the following commands run the respective specs:

    $ ../mspec/bin/mspec :core
    $ ../mspec/bin/mspec :library
    $ ../mspec/bin/mspec :capi

### Contributing

See [CONTRIBUTING.md](https://github.com/ruby/rubyspec/blob/master/CONTRIBUTING.md).
