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

### Incremental compiling

After changing Java code, you can recompile quickly by running:

```
mvn package
```

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

### maven integration tests - -Pjruby-complete or -Pmain

maven integration test will use the packed maven artifact to run the tests in a forked maven instance. these maven projects are locatated in

```
maven/jruby/src/it
maven/jruby-complete/src/it
maven/jruby-jars/src/it
maven/jruby-dist/src/it
```

to trigger the tests with the build:

```
mvn -Pmain -Dinvoker.skip=false
mvn -Pcomplete -Dinvoker.skip=false
mvn -Pdist -Dinvoker.skip=false
mvn -Pjruby-jars -Dinvoker.skip=false
```

to pick a particular test add the name of the directory inside the respective *src/it* folder, like (wildcards are possible):


```
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=integrity
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=j2ee*
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=osgi*
```

Clean Build
-----------

To clean the build it is important to use the same profile for the clean as what you want to build. the best way to clean build something is, i.e. jruby-jars

```
mvn clean install -Pjruby-jars
```

this first cleans everything and then starts the new build in one go !

Cleaning the build may be necessary after switching to a different
version of JRuby (for example, after switching git branches) to ensure
that everything is rebuilt properly.

NOTE: ```mvn clean``` just cleans the **jruby-core** artifact and the **./lib/jruby.jar** !

clean everything:

```
mvn -Pclean
```

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

the gem will be in ./maven/jruby-jars/pkg


### building ALL packages ###

```
mvn -Pall
```

## release ##

first set the new version in the file *VERSION* inside the root directory and then

```
rake maven:dump_poms
```
or
```
rmvn validate -Pall
```

manually rollback the poms in ./ext/ if their main versions have been changed
and then commit and tag averything respectively.  Now deploy the maven 
artifact to sonatype oss.

```
mvn clean deploy -Psonatype-oss-release -Prelease
```

go to oss.sonatype.org and close the deployment which will check if all 'required' files are in place and then finally push the release to maven central and . . . 

### Start a new version

After the release set the new development version in *VERSION* and generate the pom.xml files

```
rake maven:dump_poms
```
or
```
rmvn validate -Pall
```
