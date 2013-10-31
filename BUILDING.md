Building JRuby from Source
==========================

NOTE: needs maven-3.x

JRuby uses Maven for building and bootstrapping itself, along with Rake,
RSpec, and MSpec for running integration tests.

Bootstrapping JRuby
-------------------

The first time you enter a new source dump of JRuby (from a src zip or
from a git clone), you will want to fully bootstrap the environment. The
command to execute is:

```
mvn
```

Or if you prefer to be more explicit, the default "package" goal can
be specified:

```
mvn package
```

This will do all of the following:

* Compile JRuby
* Build `lib/jruby.jar`, needed for running at command line
* Install the jruby-launcher gem to provide a native 'jruby' executable.

The environment is now suitable for running Ruby applications.

Bootstrapping only needs to be done once at first entry into a JRuby
source dump or if you are updating JRuby from a git repository.

Running JRuby
-------------

*Note: RVM users must first run:*

```
rvm use system
```

*to make sure you do not use another Ruby's gems or execute another Ruby implementation.*

Once bootstrapped, JRuby can be run with the `bin/jruby` executable. If
the `jruby-launcher` gem installed successfully, this will be a native
executable for your platform; otherwise, it will be a copy of the
`bin/jruby.bash` bash script.

Bootstrapping will install the following gems:

* `rake`
* `rspec`
* `jruby-launcher`
* `minitest`
* `minitest-excludes`
* `rdoc`

and dependencies of these gems. A list of the gem versions can be found in
`test/pom.xml` in the `dependencies` section.

RubyGems is installed by default, and available in `bin/gem`. It will
attempt to locate the `jruby` executable using `/usr/bin/env`, so you
will need the `bin` dir in your `PATH` environment or you will need to
call it via JRuby using `jruby -S gem ...`.

The `-S` flag will run any script installed in JRuby's bin dir by RubyGems.
This can be a simple way to ensure you're running the JRuby (or Ruby) version
you think you are.

Developing and Testing
----------------------

JRuby employs a large suite of tests, so there are many ways you can
verify that JRuby is still fully functional.

### Bootstrapping

In order to prepare JRuby for testing, you must bootstrap the dev
environment. This will do the following:

* Install rspec, rake, minitest, minitest-excludes, and dependencies
 needed to run integration tests.

```
mvn -Pbootstrap
```

This only needs to be run once to install these gems or if you update
one of the gems to a newer version or clean out all installed gems.

### Day to Day Testing

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

Clean Build
-----------

To clean the build it is important to use the same profile for the clean as what you want to build. the best way to clean build something is, i.e. jruby-jars

```
mvn clean install -Pjruby-jars
```

this first cleans everything and then starts the new build in one go !

NOTE: ```mvn clean``` just cleans the **jruby-core** artifact and the **./lib/jruby.jar** !

Distribution Packages
---------------------

###the tar.gz and zip distribution packages###

```
mvn -Pdist
```

the files will be found in ./maven/jruby-dist/target

###jruby-complete.jar###

```
mvn -Pcomplete
```

the file will be in ./maven/jruby-complete/target

###jruby maven artifacts###

```
mvn -Pmain
```

and those files will be installed in you maven local-repository ready to use with maven, ivy, buildr, etc

###jruby jars gem###

```
mvn -Pjruby-jars
```

the gem will be in ./maven/jruby-jars/target

### gems - excluding jruby jars gem###

```
mvn -Pgems
```

the gem will be in ./maven/gems/*/pkg

### building ALL packages ###

```
mvn -Pall
```

## release ##

first set the new version:

```
mvn versions:set -DnewVersion=1.7.5 -Pall
```

manually rollback the poms in ./ext/ if their main versions have been changed
and then commit and tag averything respectively.  Now deploy the maven 
artifact to sonatype oss.

```
mvn clean deploy -Psonatype-oss-release -Prelease
```

go to oss.sonatype.org and close the deployment which will check if all 'required' files are in place and then finally push the release to maven central and . . . 
