#-*- mode: ruby -*-
# vim: syntax=Ruby

group_id 'org.jruby'
artifact_id 'jruby'
version '1.7.5.dev'
packaging 'jar'
name 'JRuby'

repository( 'localrepo' ).url 'file:./localrepo'

source_control do |sc|
  u = 'github.com/jruby/jruby'
  sc.connection "scm:git:git://#{u}.git"
  sc.developer_connection "scm:git:ssh://git@#{u}.git"
  sc.url "http://#{u}"
end

jar 'org.jruby.joni:joni', '2.0.0'
jar 'com.github.jnr:jnr-netdb', '1.1.2'
jar 'com.github.jnr:jnr-enxio', '0.4'
jar 'com.github.jnr:jnr-unixsocket', '0.3'
jar 'com.github.jnr:jnr-posix', '3.0.0'
jar 'org.jruby.extras:bytelist', '1.0.10'
jar 'com.github.jnr:jnr-constants', '0.8.4'
jar 'org.jruby.jcodings:jcodings', '1.0.10'
jar 'com.github.jnr:jffi', '1.2.5'
jar 'com.github.jnr:jnr-ffi', '1.0.4'
jar 'org.yaml:snakeyaml', '1.11'
jar( 'jline:jline', '2.7' ).scope :provided
jar 'joda-time:joda-time', '2.3'
jar 'com.jcraft:jzlib', '1.1.2'
jar 'com.headius:invokebinder', '1.2'
jar( 'org.bouncycastle:bcpkix-jdk15on', '1.47' ).scope :provided
jar( 'org.bouncycastle:bcprov-jdk15on', '1.47' ).scope :provided
jar( 'org.osgi:org.osgi.core', '4.3.1' ).scope :provided
jar( 'org.apache.ant:ant', '1.8.4' ).scope :provided
jar( 'jay:yydebug', '1.0' )
jar( 'org.apache.bsf:bsf', '1.0' ).scope :provided
jar( 'nailgun:nailgun', '0.7.1' )
jar( 'coro:coro-mock', '1.0-SNAPSHOT' ).scope :provided
jar( 'sun.misc:unsafe-mock', '1.0-SNAPSHOT' ).scope :provided
# jar 'com.github.jnr:jnr-ffi', '1.0.4'

build.source_directory = 'src'
build.output_directory = 'build/classes/jruby'

res = build.resources
res.add do |r|
  r.directory 'src'
  r.includes << '**/*rb'
end
res.add do |r|
  r.target_path '${tzdata.build.directory}'
  r.directory '${project.basedir}/resources'
  r.includes << '.empty'
end
res.add do |r|
  r.target_path '${generated.sources}'
  r.filtering true
  r.directory '${project.basedir}/resources'
  r.includes << '${Constants.java}'
end

# NOTE for rake, ant and make users
# maven is basically a declaration of sequence of "tasks". each tasks gets
# associated with a phase and maven executes the phases in a given order.
# within the same phase the "tasks" get executed in the order they got
# declared.
#
# in this file the order things get executed is the same as they are declared
# with maybe one or the other exception (the compiler plugin gets executed
# several times so a plugin might need to be declared before the compiler if
# in a certain phase the compiler should come second )
#
# maven default phase sequence:
#  validate, initialize, generate-sources, process-sources, generate-resources,
#  process-resources, compile, process-classes, generate-test-sources,
#  process-test-sources, generate-test-resources, process-test-resources,
#  test-compile, process-test-classes, test, prepare-package, package,
#  pre-integration-test, integration-test, post-integration-test, verify,
#  install, deploy

# phase: initialize
plugin( 'org.codehaus.mojo:properties-maven-plugin', '1.0-alpha-2' )
  .in_phase( 'initialize', 'properties' )
  .execute_goal( 'read-project-properties' )
  .with( :files => [ '${basedir}/default.build.properties',
                     '${basedir}/build.properties' ],
         :quiet => true )

# phase: generate-sources
plugin( 'org.codehaus.mojo:build-helper-maven-plugin', '1.8' )
  .in_phase( 'generate-sources', 'add-generated-sources' )
  .execute_goal( 'add-source' )
  .with( :sources => [ '${generated.sources}' ] )

# phase: process-sources
plugin( 'org.codehaus.mojo:buildnumber-maven-plugin', '1.2' )
  .in_phase( 'process-sources', 'jruby-revision' )
  .execute_goal( 'create' )
  .with( :shortRevisionLength => 7,
         :buildNumberPropertyName => 'jruby.revision' )

# this one must execute before the compiler goal of the same phase
# i.e. it needs to be declared before the first compiler plugin
plugin( 'org.codehaus.mojo:exec-maven-plugin', '1.2.1' ) do |pl|
  pl.in_phase( 'process-classes', 'invoker-generator' )
    .execute_goal( 'exec' )
    .with( :arguments => [ '-Djruby.bytecode.version=${javac.version}',
                           '-classpath',
                           '<classpath />',
                           'org.jruby.anno.InvokerGenerator',
                           '${anno.sources}/annotated_classes.txt',
                           '${project.build.outputDirectory}' ],
           :executable => 'java',
           :classpathScope => 'compile' )
  pl.in_phase( 'process-classes', 'unsafe-generator' )
    .execute_goal( 'exec' )
    .with( :arguments => [ '-classpath',
                           '<classpath />',
                           'org.jruby.util.unsafe.UnsafeGenerator',
                           'org.jruby.util.unsafe',
                           '${project.build.outputDirectory}' ],
           :executable => 'java' )
end

# phase: process-resources
plugin :compiler, '3.1' do |pl|
  # common compiler config for all executions
  pl.with( :encoding => 'utf-8',
           :debug => true,
           :verbose => true,
           :fork => true,
           :showWarnings => true,
           :showDeprecation => true,
           :source => "${javac.version}",
           :target => "${javac.version}" )
  pl.in_phase( 'process-resources', 'anno' ).execute_goal( :compile )
    .with( :includes => [ 'org/jruby/anno/FrameField.java',
                          'org/jruby/anno/AnnotationBinder.java',
                          'org/jruby/anno/JRubyMethod.java',
                          'org/jruby/anno/FrameField.java',
                          'org/jruby/CompatVersion.java',
                          'org/jruby/runtime/Visibility.java',
                          'org/jruby/util/CodegenUtils.java',
                          'org/jruby/util/SafePropertyAccessor.java' ] )
  pl.in_phase( 'process-resources', 'constants' ).execute_goal( :compile )
    .with( :includes => [ '${Constants.java}' ] )
end

plugin( :dependency, '2.8' ).in_phase( 'process-resources', 'copy-unsafe' )
  .execute_goal( 'copy' )
  .with( :artifactItems => [ { :groupId => 'sun.misc',
                               :artifactId => 'unsafe-mock',
                               :version => '1.0-SNAPSHOT',
                               :type => :jar,
                               :overWrite => false,
                               :outputDirectory => '${project.build.directory}',
                               :destFileName => 'unsafe.jar' } ] )

# phase: compile
# the name 'default-compile' is important as it replaces maven predefined goal
plugin( :compiler ).in_phase( 'compile', 'default-compile' )
  .execute_goal( :compile )
  .with( :excludes => [ '${Constants.java}' ],
         :annotationProcessors => [ 'org.jruby.anno.AnnotationBinder' ],
         :compilerArgs => [ '-XDignore.symbol.file=true',
                            '-J-Duser.language=en',
                            '-J-Dfile.encoding=UTF-8',
                            '-J-Xbootclasspath/p:${unsafe.jar}' ] )

# phase: process-classes
plugin( 'org.codehaus.mojo:build-helper-maven-plugin' )
  .in_phase( 'process-classes', 'add-populators' )
  .execute_goal( 'add-source' )
  .with( :sources => [ '${anno.sources}' ] )

plugin( :compiler ).in_phase( 'process-classes', 'populators' )
    .execute_goal( :compile )
    .with( :includes => [ 'org/jruby/gen/**/*java' ] )

plugin( :clean, '2.5' )
  .in_phase( 'process-classes', 'clean-anno-config' ).execute_goal( :clean )
  .with( :excludeDefaultDirectories => true,
         :filesets => [ { :directory => '${anno.sources}',
                          :includes => [ 'annotated_classes.txt' ] } ],
         :failOnError => false )
plugin( :dependency, '2.8' ).in_phase( 'process-classes', 'unzip native' )
  .execute_goal( 'unpack' )
  .with( :excludes => 'META-INF,META-INF/*',
         :artifactItems => [ { :groupId => 'com.github.jnr',
                               :artifactId => 'jffi',
                               :version => '1.2.5',
                               :type => :jar,
                               :classifier => :native,
                               :overWrite => false,
                               :outputDirectory => '${basedir}/lib' } ] )

# phase: package
plugin( :jar, '2.3.2' )
  .in_phase( :package, 'default-jar' )
  .execute_goal( :jar )
  .with( :excludes => [ '**/*openssl*/**/*',
                        'org/jruby/gen/org$jruby$ext$openssl*',
                        'org/jruby/ext/readline/**/*',
                        'org/jruby/demo/readline/**/*',
                        'org/jruby/gen/org$jruby$ext$readline*' ],
         :archive => {
           :manifest => {
             :mainClass => 'org.jruby.Main'
           }
         } )
plugin( :assembly, '2.4' )
  .in_phase( :package )
  .execute_goal( :single )
  .with( :descriptors => [ '${basedir}/resources/jar-with-dependencies.xml' ],
         :finalName => 'jruby',
         :outputDirectory => '${basedir}/lib' )
#plugin( 'org.sonatype.plugins:jarjar-maven-plugin', '1.7' )
#  .in_phase( :package, 'jarjar-native' )
#  .execute_goal( :jarjar )
#  .with( :includes =>  ['com.github.jnr:jffi' ], 
#         :rules => [] )


properties[ 'anno.sources' ] = '${project.basedir}/build/src_gen'
properties[ 'unsafe.jar' ] = '${project.build.directory}/unsafe.jar'
properties[ 'generated.sources' ] =
  '${project.build.directory}/generated-sources'
properties[ 'project.build.sourceEncoding' ] = 'utf-8'
properties[ 'maven.build.timestamp.format' ] = 'yyyy-MM-dd'
properties[ 'build.date' ] = '${maven.build.timestamp}'
properties[ 'tzdata.tar.gz' ] = '${project.build.directory}/tzdata.tar.gz'
properties[ 'tzdata.build.directory' ] =
  '${project.build.directory}/tzdata/build/org/joda/time/tz/data'
properties[ 'Constants.java' ] = 'org/jruby/runtime/Constants.java'

profile( 'tzdata' ) do |pr|

end
