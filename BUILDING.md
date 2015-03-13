Building JRuby from Source
==========================

Prerequisites:

* A Java 7-compatible (or higher) Java development kit (JDK).
  MAKE SURE JAVA_HOME IS SET ON OS X!
* Maven 3+
* Apache Ant 1.8+ (see https://github.com/jruby/jruby/issues/2236)
* make and a C++ compiler for installing the jruby-launcher gem

JRuby uses Maven for building and bootstrapping itself, along with Rake,
RSpec, and MSpec for running integration tests.

Building Commandline JRuby
--------------------------

The first time you enter a new source dump of JRuby (from a src zip or
from a git clone), you need to build the lib/jruby.jar. The
command to execute is:

```
mvn
```

This will run the default "install" goal (```mvn install```) and will do all of the following:

* Compile JRuby
* Compile JRuby-Truffle
* Build `lib/jruby.jar`, needed for running at command line
* It will install the default gems specifications `lib/ruby/gems/shared/specifications/default/` and the ruby files of
  those gems in `lib/ruby/stdlib/`.

The environment is now suitable for running Ruby applications.

Running JRuby
-------------

*Note: RVM users must first run:*

```
rvm use system
```

*to make sure you do not use another Ruby's gems or execute another Ruby implementation.*

After building lib/jruby.jar, JRuby can be run with the `bin/jruby` executable. If the `jruby-launcher` gem installed successfully, this will be a native
executable for your platform; otherwise, it will be a copy of the
`bin/jruby.bash` bash script.

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

After changing Java code, you can recompile quickly by running:

```
mvn
```

### Day to Day Testing

For normal day-to-day testing, we recommend running the Ruby (MRI) tests
via the following rake command:

```
bin/jruby -S rake test:mri
```

This suite takes a while to complete, so if you want to run an individual file
from MRI's tests (under test/mri), use one of the following commands:

# Run a specific test method in a specific file
```
jruby <test file> -n <specific test method>
```

# Run a test file with known-failing tests excluded
```
EXCLUDES=test/mri/excludes bin/jruby -r test/mri_test_env.rb test/mri/runner.rb -q -- <test file>
```
#### Run a single spec
```
bin/jruby spec/mspec/bin/mspec run spec/ruby/core/symbol/length_spec.rb
```

#### Run a single spec with remote debugging
```
bin/jruby spec/mspec/bin/mspec run -T-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 spec/ruby/core/symbol/length_spec.rb
```

Additional tests may be run through mspec.
```
bin/jruby -S mspec -B spec/jruby.2.2.mspec -t bin/jruby -G fails ci <test files>
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

### cleaning the build ###

this will also clean the **ext** directories, i.e. a new build will then use the latest code from there for **lib/ruby**

```
jruby -S rmvn -Pclean
```

## release ##

first set the new version in the file *VERSION* inside the root directory and then

```
rake maven:dump_poms
```
or
```
rmvn installe -Pall
```

Now deploy the maven 
artifact to sonatype oss.

```
mvn clean deploy -Psonatype-oss-release
```

go to oss.sonatype.org and close the deployment which will check if all 'required' files are in place and then finally push the release to maven central and . . . 

hacking the build system
------------------------

the build system uses the **ruby-maven** gem and with this the build files are **pom.rb** and **Mavenfile**. the **Mavenfile** are used whenever the module produces a gem and uses the gemspec file for the gem for setting up the POM. otherwise **pom.rb** are used. so any change in the build-system is done in those files !!!!

instead of ```mvn``` the ```rmvn``` command is used. this command will also generate **pom.xml** files which can be used by regular maven.

to (re)generate all pom.xml use
```
rake maven:dump_poms
```
(which is basically ```rmvn validate -Pall```)

about the ruby DSL for those poms just look in the existing pom.rb/Mavenfile files - there are plenty of examples for all kind of situations. (more documentation to come).

regular maven uses the the jruby from the installation, i.e. 9.0.0.0. this also means that a regular maven run does not depend under the hood on any other jruby versions from maven central.

at some parts there are **inline** plugins in **pom.rb** or **Mavenfile** which will work directly with regular maven where there is a special plugin running those ruby parts. see **./lib/pom.rb**.

### Start a new version

After the release set the new development version in *VERSION* and generate the pom.xml files

```
rake maven:dump_poms
```
or
```
rmvn install -Pall
```
