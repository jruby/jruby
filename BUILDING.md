Building JRuby from Source
==========================

Prerequisites:

* A [Java 8-compatible (or higher) Java development kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
  * If `JAVA_HOME` is not set on Mac OS X: `export JAVA_HOME=$(/usr/libexec/java_home)`
* [Maven](https://maven.apache.org/download.cgi) 3.3.0+ (Maven Wrapper provided with `./mvnw`)
* [Apache Ant](https://ant.apache.org/bindownload.cgi) 1.8+ (see https://github.com/jruby/jruby/issues/2236)
* [Make](https://www.gnu.org/software/make/) and a C++ compiler for installing the jruby-launcher gem

JRuby uses Maven for building and bootstrapping itself, along with Rake,
RSpec, and MSpec for running integration tests.

Building Commandline JRuby
--------------------------

The first time you enter a new source dump of JRuby (from a src zip or
from a git clone), you need to build the lib/jruby.jar. The
command to execute is:

```
./mvnw
```

This will run the default "install" goal (`mvn install`) and will do all of the following:

* Compile JRuby
* Build `lib/jruby.jar`, needed for running at command line
* It will install the default gems specifications `lib/pom.rb` and the ruby files of those gems in `lib/ruby/stdlib/`.

The environment is now suitable for running Ruby applications.

If you have Maven installed in your PATH, you can just use `mvn` instead of `./mvnw`.

Incremental Builds
------------------

When working on JRuby sources, it is helpful to incrementally rebuild only the `lib/jruby.jar` file rather than also
re-assembling the standard library. You can add `-Dcore` to the `mvn` command line to speed up incremental builds:

```
./mvnw -Dcore
```

Running JRuby
-------------

*Note: RVM users must first run:*

```
rvm use system
```

*to make sure you do not use another Ruby's gems or execute another Ruby implementation.*

After building lib/jruby.jar, JRuby can be run with the `bin/jruby` executable. If the `jruby-launcher` gem installed successfully, this will be a native
executable for your platform; otherwise, it will be a copy of the
`bin/jruby.sh` shell script.

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

### Hacking the Build System

for a general overview of the different directories and maven artifacts see [JRuby Build](https://github.com/jruby/jruby/wiki/JRuby-Build----Some-Inside-Info)

For this only  the ***pom.rb*** needs to edited. using mvn-3.3.x or the maven wrapper `./mvnw` will generate the pom.xml file where needed. For the jar files of the build those pom.xml will be generated for some use-cases, i.e. some IDEs need them.

To regenerate the pom.xml just run `./mvnw` which will create them.

### Setup Testing

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

After changing Java code, you can recompile quickly by running one of the
jar files by

```
mvn -pl core
```

### Day to Day Testing

For normal day-to-day testing, we recommend running the Ruby specs. We have set aside a
"fast" grouping that takes only a couple minutes to run:

```
./bin/jruby -S rake spec:ruby:fast
```

For a more intensive workout, you can also run the Ruby (MRI) tests
via the following rake command:

```
./bin/jruby -S rake test:mri
```

This suite takes a while to complete, so if you want to run an individual file
from MRI's tests (under test/mri), use one of the following commands:

#### Run a specific test from the MRI suite

The MRI suite (under `test/mri`) has a runner script in `test/mri/runner.rb` that sets up
an appropriate test environment. Many of the MRI tests will need to be run via this script.
```
./bin/jruby test/mri/runner.rb test/mri/<path to test>
```

You can pass `-v` to the runner for verbose output or `-n test_method_name` to only run a single test method.

#### Run a test file with known-failing tests excluded

The runner script provides a mechanism for "excluding" known failing tests. These are usually features that JRuby has not yet implemented or can't implement on the JVM.

Excludes are in the form of Ruby scripts under `test/mri/exclude`, named based on the name of the test case's class, exclude with comment tests known to fail.

To run a given test with these excludes enabled, you can use the --excludes flag:

```
./bin/jruby test/mri/runner.rb --excludes=test/mri/excludes <test file>
```

#### Run a single spec using RSpec

Most of the specs under the spec/ directory are written for rspec, and can be run with rspec.

The notable exception is the "Ruby specs" under spec/ruby, which are run with mspec as described later in this document.

rspec will be installed with `mvn package -Pbootstrap` or you can install it manually.

```
./bin/jruby -S rspec spec/path/to/spec
```

#### Run a single "Ruby spec" using mspec

The specs under spec/ruby are part of the "Ruby spec" suite of tests and use the "mspec" tool to run.

Individual specs can be run with the mspec tool:

```
./bin/jruby spec/mspec/bin/mspec ci spec/ruby/<path to spec>
```

If `ci` is omitted or replaced with `run` you will see any specs known to fail. The `ci` command
avoids running those specs.

#### Run JRuby with remote debugging

If you are familiar with Java debuggers, you can attach one to a JRuby process using the JDWP agent.
The exact flag may vary with debugger and platform:

```
JRUBY_OPTS="-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005" ./bin/jruby <rest of arguments>
```
#### JRuby internal unit tests

If you are making changes that would affect JRuby's core runtime
or embedding APIs, you should run JRuby's Java-based unit tests via

```
mvn -Ptest
```

#### Tests for other ways of deploying and packaging JRuby

There are some maven integration tests (i.e. consistency test if all gems are included, osgi test, etc) for the various distributions of JRuby which can be invoked with

```
mvn -Pmain -Dinvoker.skip=false
mvn -Pcomplete -Dinvoker.skip=false
mvn -Pdist -Dinvoker.skip=false
```

#### Just Like CI

JRuby runs CI tests on GitHub CI. See [.github/workflows](https://github.com/jruby/jruby/blob/master/.github/workflows).

#### Maven integration tests - -Pjruby-complete or -Pmain

maven integration tests will use the packed maven artifact to run the tests in a forked maven instance. These maven projects are locatated in

```
maven/jruby/src/it
maven/jruby-complete/src/it
maven/jruby-jars/src/it
maven/jruby-dist/src/it
```

To trigger the tests with the build:

```
mvn -Pmain -Dinvoker.skip=false
mvn -Pcomplete -Dinvoker.skip=false
mvn -Pdist -Dinvoker.skip=false
mvn -Pjruby-jars -Dinvoker.skip=false
```

To pick a particular test, add the name of the directory inside the respective *src/it* folder, like (wildcards are possible):

```
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=integrity
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=j2ee*
mvn -Pmain -Dinvoker.skip=false -Dinvoker.test=osgi*
```

Clean Build
-----------

To clean the build it is important to use the same profile for the clean as what you want to build. The best way to clean build something is, i.e. jruby-jars

```
mvn clean install -Pjruby-jars
```

This first cleans everything and then starts the new build in one go!

Cleaning the build may be necessary after switching to a different
version of JRuby (for example, after switching git branches) to ensure
that everything is rebuilt properly.

NOTE: `mvn clean` just cleans the **jruby-core** artifact and the **./lib/jruby.jar**!

Clean everything:

```
mvn -Pclean
```

Distribution Packages
---------------------

All distribution packages need maven-3.3.x or the use of supplied maven wrapper. All examples below will show the use of the maven wrapper.

### The tar.gz and zip distribution packages

```
./mvnw -Pdist
```

The files will be in `./maven/jruby-dist/target`.

### `jruby-complete.jar`

```
./mvnw -Pcomplete
```

The file will be in `./maven/jruby-complete/target`.

### jruby maven artifacts

```
./mvnw -Pmain
```

And those files will be installed in your maven local-repository ready to use with maven, ivy, buildr, etc.

### jruby jars gem

```
./mvnw -Pjruby-jars
```

The gem will be in `./maven/jruby-jars/pkg`.

### Building ALL packages

```
./mvnw -Pall
```

### Cleaning the build

This will also clean the **ext** directories, i.e. a new build will then use the latest code from there for **lib/ruby**.

```
./mvnw -Pclean
```

## Release

First set the new version in the file *VERSION* inside the root directory and then to deploy the maven artifact to sonatype oss execute:

```
./mvnw clean deploy -Psonatype-oss-release
```

Go to https://oss.sonatype.org/ and close the deployment, which will check if all 'required' files are in place and then finally push the release to Maven Central and . . . 

### Start a new version

After the release, set the new development version in *VERSION* and generate the `pom.xml` files:

```
./mvnw
```
