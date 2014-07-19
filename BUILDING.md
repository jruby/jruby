Building JRuby from Source
==========================

Prerequisites:

* A Java 7-compatible (or higher) Java developement kit (JDK)
* Maven 3+

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
* It will install the default gems specifications `lib/ruby/gems/shared/specifications/default/` and the ruby files of those gems in `lib/ruby/shared/` and `lib/ruby/2.1/`.

The environment is now suitable for running Ruby applications.

Bootstrapping only needs to be done once at first entry into a JRuby
source dump or if you are updating JRuby from a git repository.

The list of the default gems can be found at the beginning of `lib/pom.rb`.

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

In case there is a problem with installing the jruby-launcher (due to missing compiler or so) use

```
mvn -Pbootstrap-no-launcher
```


This only needs to be run once to install these gems or if you update
one of the gems to a newer version or clean out all installed gems.

### Incremental compiling

After changing Java code, you can recompile quickly by running:

```
mvn
```

If you only want to build JRuby core (everything that goes in jruby.jar), you can use
the following command:

```
mvn -pl core
```

This is generally the quickest way to build when you are just modifying JRuby core
classes.

### Day to Day Testing

For normal day-to-day testing, we recommend running the Ruby (MRI) tests
via the following rake command:

```
bin/jruby -S rake test:mri
```

This suite takes a while to complete, so if you want to run an individual file
from MRI's tests (under test/mri), use one of the following commands:

```
# Run a specific test method in a specific file
jruby <test file> -n <specific test method>

# Run a test file with known-failing tests excluded
EXCLUDE_DIR=test/mri/excludes jruby -r minitest/excludes <test file>
```

For more complete assurance, you can also run 1.9 RubySpecs via the
following command:

```
jruby spec/mspec/bin/mspec ci
```

And if you are making changes that would affect JRuby's core runtime
or embedding APIs, you should run JRuby's Java-based unit tests via

```
mvn -Ptest
```

On travis the following tests will run

```
mvn -Ptest
mvn -Prake -Dtask=test:extended
mvn -Prake -Dtask=spec:ci\_interpreted\_travis
mvn -Ptruffle
```

There are some maven integration tests (i.e. consistency test if all gems are included, osgi test, etc) for the various distributions of JRuby which can be invoked with

```
mvn -Pmain -Dinvoker.skip=false
mvn -Pcomplete -Dinvoker.skip=false
mvn -Pdist -Dinvoker.skip=false
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

Cleaning the build may be necessary after switching to a different
version of JRuby (for example, after switching git branches) to ensure
that everything is rebuilt properly.

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

### building ALL packages ###

```
mvn -Pall
```

### cleaning the build ###

this will also clean the **ext** directories, i.e. a new build will then use the latest code from there for **lib/ruby**

```
mvn -Pclean
```

## release ##

first set the new version (on jruby-1_7 branch):

```
mvn versions:set -DnewVersion=1.7.5 -Pall
```

on master you need to run
```
rake maven:set_version
```

manually rollback the poms in ./ext/ if their main versions have been changed
and then commit and tag averything respectively.  Now deploy the maven 
artifact to sonatype oss.

```
mvn clean deploy -Psonatype-oss-release -Prelease
```

go to oss.sonatype.org and close the deployment which will check if all 'required' files are in place and then finally push the release to maven central and . . . 

# hacking the build system #

the build system uses the **ruby-maven** gem and with this the build files are **pom.rb** and **Mavenfile**. the **Mavenfile** are used whenever the module produces a gem and uses the gemspec file for the gem for setting up the POM. otherwise **pom.rb** are used. so any change in the build-system is done in those files !!!!

instead of ```mvn``` the ```rmvn``` command is used. this command will also geneate **pom.xml** files which can be used by regular maven.

to (re)generate all pom.xml use
```
rake maven:dump_poms
```
(which is basically ```rmvn validate -Pall```)

about the ruby DSL for those poms just look in the existing pom.rb/Mavenfile files - there are plenty of examples for all kind of situations. (more documention to come).

regular maven uses the the jruby from the installion, i.e. 9000.dev. this also means that a regular maven run does not depend under the hood on any other jruby versions from maven central.

at some parts there are **inline** plugins in **pom.rb** or **Mavenfile** which will work directly with regular maven where there is a special plugin running those ruby parts. see **./lib/pom.rb**.

