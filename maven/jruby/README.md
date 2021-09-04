# running tests #

all tests from the ./maven/jruby directory
```
mvn verify -Papps
```
or from the jruby root directory
```
mvn -Papps
```


to pick a single tests
```
mvn verify -Papps -Dinvoker.test=hellowarld_jetty_unpacked_warfile_cuba
```

see the directory listing of ```./src/it/hellowarld_*``` - each directory is a single test.

you also can use wildcards to pick test runs
```
mvn verify -Papps -Dinvoker.test=hellowarld_*_cuba
mvn verify -Papps -Dinvoker.test=hellowarld_jetty_*
mvn verify -Papps -Dinvoker.test=hellowarld_jetty_*rails4
mvn verify -Papps -Dinvoker.test=hellowarld*warfile*sinatra
```

## the application is implemented with different web-framework

it is a simple application demonstrating the PATCH http verb and adds various monitoring rack plugins which uses quite some numbers of jar dependencies.

### cuba

does not use bundler and almost a pure rack application as cuba has tiny codebase.

### sinatra

uses bundler (even it could just do the same as the cuba app). bundler adds another workaround here in there inside the packed applications.

### rails4

rails is heavily bound to its filesystem layout which can cause problems with packing it in jar/war and run it from there (without unpacking it).

## webservers no servlets

### webrick

default from ruby

### puma

something which is working with jruby

### torquebox

it comes with its own tools to pack jars and wars which is NOT used. but it executes a config.ru like the other to webservers

## webservers - servlets engine

### jetty

### tomcat

### wildfly

it can run war-files packed or deploy them which includes an unpacking on disk. both cases are tested

## package format

### filesystem

using one of the webserver (not servlets)

### runnable

pack the whole application into a jar file and execute the webserver from within the jar and run the whole application from within the jar

### warfile

needs a servlet container to run

# problems

* nokogiri (from rails) does not work with jdk8 with servlets since it
  usese com.sun.* classes which do not exist anymore. not sure why it
  works outside a servelt engine - at least with wildfly

* with jdk7 rails on servlet needs more perm space

# PENDING

* websphere which just needs more copy paste from another template here

* rails5

* glassfish, resin would be nice to have as well

* ear files with jruby part of the ear-container and then war-container inherits this classpath

* web-archive-bundle, i.e. OSGi bundle with a web-application packed. here jruby could be external dependency and its own OSGi bundle

* actually trigger the PATCH request and test its outcome

* run jetty and tomcat both packed and unpacked. probably needs just some custom context.xml

* get the jruby9 extensions into the ruby DSL and clean up the runy DSL a bit (jars_lock to pick the jar dependencies from there, etc).
