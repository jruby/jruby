# Class Loading in JRuby #

## JRubyClassLoader ##

each JRuby runtime has its own JRubyClassLoader and all the required jar like ```require 'my.jar'``` will be added to this JRubyClassLoader.

in JRuby 1.7.x and before the JRubyClassLoader has a parent first strategy to load classes, later versions of JRuby use a "self-first" strategy to load classes. the latter separates the required jars from the underlying classloaders. i.e. if the java application uses an ancient "jline.jar" then the JRuby will use its own jline.jar version !

the parent classloader of JRubyClassLoader is the one which can load jruby itself. the parent can be ```Thread.currentThread.getContextClassLoader``` or the classloader ```org.jruby.Ruby.class.getClassLoader``` (which could be the same classloader any way).

## uri:classloader: protocol ##

this parent classloader of the JRubyClassLoader can be referenced by ***uri:classloader:***. the first entry of ```$LOAD_PATH``` is ***uri:classloader:*** which is needed to the kernel of JRuby.

## jruby.home ##

unless the jruby.home gets set via the properties/config/env it will be

* uri:classloader:/META-INF/jruby.home/lib/ruby/1.9/site_ruby
* uri:classloader:/META-INF/jruby.home/lib/ruby/shared
* uri:classloader:/META-INF/jruby.home/lib/ruby/1.9

which is the same for all environments (java -jar jruby-complete.jar, java application, j2ee, osgi, etc).

## embedded ruby scripts and ruby gems inside jars ##

any jar of the classpath (or the same classloader where jruby is loaded from) can have ruby scripts/resources. as long there is no directory globs needed inside jars those ruby scripts/resources will work for **ALL** classloaders.

to get the directory globs working for **ALL** classloaders the jars need to provide the directory entries to JRuby. having a file **.jrubydir** inside the directory with all directories will do the trick.

to generate such **.jrubydir** files just execute

     jruby -r jruby/commands -e "JRuby::Commands.generate_dir_info('path/to/dir')"

for embedded ruby gems this extra **.jrubydir** is essential to work accross all java frameworks, like certain j2ee server, osgi containers, etc.

## adding jars to the $LOAD_PATH ##

when creating a new JRuby runtime the jars from the classpath will be added to $LOAD explicitly. this can be not what you expect or want: it adds the bootstrap classpath of your environment (j2ee server, osgi, etc) to the $LOAD_PATH which can be totally unrelated to your jruby application.

further whenever you require a jar inside the ruby code this jar will be added to the $LOAD_PATH as well, i.e. all its embedded ruby scripts can be easily loaded.

both are convenient features but can lead to explode the $LOAD_PATH without benefit. to control this feature there exists following property/config: **add.jars.toLoadPath**

### jars from the classpath ###

if the ruby script inside the jars coming from the classpath (or the same classloader where jruby is loaded from) and do not use directory globs then the $LOAD_PATH entry **uri:classloader:** will find those ruby scripts. there is no need to add them explicitly to the $LOAD_PATH. dito is true when your jars will provide the **.jrubydir** files from above.

# ScriptingContainer vs. IsolatedScriptingContainer ##

the ```IsolatedScriptingContainer``` does not add any jars to the $LOAD_PATH by setting (overwriting) the system property  **jruby.add.jars.toLoadPath** to false. i.e. all embedded ruby scripts or gems inside a jar assumes to have **.jrubydir** included to work properly.

the ```Gem::Specification.dirs``` will be set to:

* uri:classloader:/specifications
* uri:classloader:/META-INF/jruby.home/lib/ruby/gems/shared/specifications

note the missing entry to the **$HOME/.gem/jruby/x.y/specifications** location. from this the ```IsolatedScriptingContainer``` got its name ;)

# Gem::Specification.dirs (or GEM_PATH) #

TODO
