Building JRuby from Source
==========================

JRuby uses Maven for building and bootstrapping itself, along with Rake,
RSpec, and MSpec for running integration tests.

Bootstrapping
-------------

The first time you enter a new source dump of JRuby (from a src zip or
from a git clone), you will want to fully bootstrap the environment. The
command to execute is:

```
mvn verify
```

This will do all of the following:

* Compile JRuby
* Build lib/jruby.jar, needed for running at command line
* Install rake, rspec, and the native "jruby" command (jruby-launcher)

The environment is now suitable for hacking JRuby and running test
targets via Rake.

Bootstrapping only needs to be done once at first entry into a JRuby
source dump or if you would like to ensure you're bootstrapped with
updated gems.

After Bootstrapping
-------------------

Once bootstrapped, you only need to rebuild the lib/jruby.jar file by
running the following command:

```
mvn
```

Or if you prefer to be more explicit, the default "package" goal can
be specified:

```
mvn package
```

Testing
-------

JRuby employs a large suite of tests, so there are many ways you can
verify that JRuby is still fully functional.

### Day to Day

For normal day-to-day testing, we recommend running the Ruby 1.9 tests
via the following rake command:

```
rake test:mri19
```

This is a reasonably good suite that does not take too long to run. For
more complete assurance, you can also run 1.9 RubySpecs via the
following command:

```
jruby spec/mspec/bin/mspec ci
```

And if you are making changes that would affect JRuby's core runtime
or embedding APIs, you should run JRuby's Java-based unit tests via

```
mvn -Ptest test
```

### Just Like CI

Our [CI](https://travis-ci.org/jruby/jruby) runs the following three commands (in essence):

```
rake test:extended
jruby spec/mspec/bin/mspec ci
jruby --1.8 spec/mspec/bin/mspec ci
```

The complete CI test suite will take anywhere from 20 to 45 minutes to
complete, but provides the most accurate indication of the stability of
your local JRuby source.
