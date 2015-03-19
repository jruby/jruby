version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Core' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-core'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,

              'tzdata.version' => '2013d',
              'tzdata.scope' => 'provided',

              'unsafe.version' => '8.0',
              'unsafe.jar' => '${settings.localRepository}/com/headius/unsafe-mock/${unsafe.version}/unsafe-mock-${unsafe.version}.jar',

              'maven.build.timestamp.format' => 'yyyy-MM-dd',
              'maven.test.skip' => 'true',
              'build.date' => '${maven.build.timestamp}',
              'main.basedir' => '${project.parent.basedir}',
              'Constants.java' => 'org/jruby/runtime/Constants.java',
              'anno.sources' => '${project.basedir}/target/generated-sources',

              'jruby.basedir' => '${basedir}/..',
              'jruby.test.memory' => '3G',
              'jruby.test.memory.permgen' => '2G',
              'jruby.compile.memory' => '2G' )

  IO.foreach(File.join(basedir, '..', 'default.build.properties')) do |line|
    line.chomp!
    # skip comments
    next if line =~ /(^\W*#|^$)/
    # build const name
    name, value = line.split("=", 2)
    properties name => value
  end

  jar 'org.ow2.asm:asm:${asm.version}'
  jar 'org.ow2.asm:asm-commons:${asm.version}'
  jar 'org.ow2.asm:asm-analysis:${asm.version}'
  jar 'org.ow2.asm:asm-util:${asm.version}'

  jar 'com.github.jnr:jnr-netdb:1.1.4'
  jar 'com.github.jnr:jnr-enxio:0.7'
  jar 'com.github.jnr:jnr-x86asm:1.0.2'
  jar 'com.github.jnr:jnr-unixsocket:0.6'
  jar 'com.github.jnr:jnr-posix:3.0.10'
  jar 'com.github.jnr:jnr-constants:0.8.6'
  jar 'com.github.jnr:jnr-ffi:2.0.2'
  jar 'com.github.jnr:jffi:${jffi.version}'
  jar 'com.github.jnr:jffi:${jffi.version}:native'

  jar 'org.jruby.joni:joni:2.1.5'
  jar 'org.jruby.extras:bytelist:1.0.12'
  jar 'org.jruby.jcodings:jcodings:1.0.13-SNAPSHOT'
  jar 'org.jruby:dirgra:0.2'

  jar 'com.headius:invokebinder:1.5'
  jar 'com.headius:options:1.1'
  jar 'com.headius:coro-mock:1.0', :scope => 'provided'
  jar 'com.headius:unsafe-mock', '${unsafe.version}', :scope => 'provided'
  jar 'com.headius:jsr292-mock:1.1', :scope => 'provided'

  jar 'bsf:bsf:2.4.0', :scope => 'provided'
  jar 'com.jcraft:jzlib:1.1.3'
  jar 'com.martiansoftware:nailgun-server:0.9.1'
  jar 'junit:junit', :scope => 'test'
  jar 'org.apache.ant:ant:${ant.version}', :scope => 'provided'
  jar 'org.osgi:org.osgi.core:5.0.0', :scope => 'provided'

  # joda timezone must be before joda-time to be packed correctly
  jar 'org.jruby:joda-timezones:${tzdata.version}', :scope => '${tzdata.scope}'
  jar 'joda-time:joda-time:${joda.time.version}'

  plugin_management do
    plugin( 'org.eclipse.m2e:lifecycle-mapping:1.0.0',
            'lifecycleMappingMetadata' => {
              'pluginExecutions' => [ { 'pluginExecutionFilter' => {
                                          'groupId' =>  'org.codehaus.mojo',
                                          'artifactId' =>  'properties-maven-plugin',
                                          'versionRange' =>  '[1.0-alpha-2,)',
                                          'goals' => [ 'read-project-properties' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } },
                                      { 'pluginExecutionFilter' => {
                                          'groupId' =>  'org.codehaus.mojo',
                                          'artifactId' =>  'build-helper-maven-plugin',
                                          'versionRange' =>  '[1.8,)',
                                          'goals' => [ 'add-source' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } },
                                      { 'pluginExecutionFilter' => {
                                          'groupId' =>  'org.codehaus.mojo',
                                          'artifactId' =>  'exec-maven-plugin',
                                          'versionRange' =>  '[1.2.1,)',
                                          'goals' => [ 'exec' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } },
                                      { 'pluginExecutionFilter' => {
                                          'groupId' =>  'org.apache.maven.plugins',
                                          'artifactId' =>  'maven-dependency-plugin',
                                          'versionRange' =>  '[2.8,)',
                                          'goals' => [ 'copy' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } },
                                      { 'pluginExecutionFilter' => {
                                          'groupId' =>  'org.apache.maven.plugins',
                                          'artifactId' =>  'maven-clean-plugin',
                                          'versionRange' =>  '[2.5,)',
                                          'goals' => [ 'clean' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } } ]
            } )
  end

  plugin 'org.codehaus.mojo:buildnumber-maven-plugin:1.2' do
    execute_goals( 'create',
                   :id => 'jruby-revision',
                   :phase => 'generate-sources',
                   'shortRevisionLength' =>  '7',
                   'buildNumberPropertyName' =>  'jruby.revision' )
  end

  phase 'process-classes' do
    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goals( 'add-source',
                     :id => 'add-populators',
                     'sources' => [ '${anno.sources}' ] )
    end

    plugin 'org.codehaus.mojo:exec-maven-plugin' do
      execute_goals( 'exec',
                     :id => 'invoker-generator',
                     'arguments' => [ '-Djruby.bytecode.version=${base.java.version}',
                                      '-classpath',
                                      xml( '<classpath/>' ),
                                      'org.jruby.anno.InvokerGenerator',
                                      '${anno.sources}/annotated_classes.txt',
                                      '${project.build.outputDirectory}' ],
                     'executable' =>  'java',
                     'classpathScope' =>  'compile' )
    end
  end

  plugin( :compiler,
          'encoding' => 'utf-8',
          'debug' => 'true',
          'verbose' => 'true',
          'fork' => 'true',
          'compilerArgs' => { 'arg' => '-J-Xmx1G' },
          'showWarnings' => 'true',
          'showDeprecation' => 'true',
          'source' => [ '${base.java.version}', '1.7' ],
          'target' => [ '${base.javac.version}', '1.7' ],
          'useIncrementalCompilation' =>  'false' ) do
    execute_goals( 'compile',
                   :id => 'anno',
                   :phase => 'process-resources',
                   'includes' => [ 'org/jruby/anno/FrameField.java',
                                   'org/jruby/anno/AnnotationBinder.java',
                                   'org/jruby/anno/JRubyMethod.java',
                                   'org/jruby/anno/FrameField.java',
                                   'org/jruby/CompatVersion.java',
                                   'org/jruby/runtime/Visibility.java',
                                   'org/jruby/util/CodegenUtils.java',
                                   'org/jruby/util/SafePropertyAccessor.java' ] )
    execute_goals( 'compile',
                   :id => 'default-compile',
                   :phase => 'compile',
                   'annotationProcessors' => [ 'org.jruby.anno.AnnotationBinder' ],
                   'generatedSourcesDirectory' =>  'target/generated-sources',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8',
                                       '-J-Xbootclasspath/p:${unsafe.jar}',
                                       '-J-Xmx${jruby.compile.memory}' ] )
    execute_goals( 'compile',
                   :id => 'populators',
                   :phase => 'process-classes',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8',
                                       '-J-Xbootclasspath/p:${unsafe.jar}',
                                       '-J-Xmx${jruby.compile.memory}' ],
                   'includes' => [ 'org/jruby/gen/**/*.java' ] )
    execute_goals( 'compile',
                   :id => 'eclipse-hack',
                   :phase => 'process-classes',
                   'skipMain' =>  'true',
                   'includes' => [ '**/*.java' ] )
  end

  plugin :clean do
    execute_goals( 'clean',
                   :id => 'default-clean',
                   :phase => 'clean',
                   'filesets' => [ { 'directory' =>  '${project.build.sourceDirectory}',
                                     'includes' => [ '${Constants.java}' ] } ],
                   'failOnError' =>  'false' )
  end

  plugin( :surefire,
          'forkCount' =>  '1',
          'reuseForks' =>  'false',
          'systemProperties' => {
            'jruby.compat.version' =>  '1.9',
            'jruby.home' =>  '${basedir}/..'
          },
          'argLine' =>  '-Xmx${jruby.test.memory} -XX:MaxPermSize=${jruby.test.memory.permgen} -Dfile.encoding=UTF-8 -Djava.awt.headless=true',
          'includes' => [ 'org/jruby/test/MainTestSuite.java',
                          'org/jruby/embed/**/*Test*.java',
                          'org/jruby/util/**/*Test*.java' ],
          'additionalClasspathElements' => [ '${basedir}/src/test/ruby' ] )

  build do
    default_goal 'package'

    resource do
      directory 'src/main/ruby'
      includes '**/*rb'
    end

    resource do
      directory 'src/main/resources'
      includes 'META-INF/**/*'
    end

    resource do
      directory '${project.basedir}/src/main/resources'
      includes '${Constants.java}'
      target_path '${project.build.sourceDirectory}'
      filtering 'true'
    end
  end


  plugin :shade do
    execute_goals( 'shade',
                   :id => 'create lib/jruby.jar',
                   :phase => 'package',
                   'relocations' => [ { 'pattern' => 'org.objectweb',
                                        'shadedPattern' => 'org.jruby.org.objectweb' } ],
                   'outputFile' => '${jruby.basedir}/lib/jruby.jar',
                   'transformers' => [ { '@implementation' => 'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                         'mainClass' => 'org.jruby.Main' } ] )
    execute_goals( 'shade',
                   :id => 'shade the asm classes',
                   :phase => 'verify',
                   'artifactSet' => {
                     'includes' => [ 'com.github.jnr:jnr-ffi',
                                     'org.ow2.asm:*' ]
                   },
                   'relocations' => [ { 'pattern' =>  'org.objectweb',
                                        'shadedPattern' =>  'org.jruby.org.objectweb' } ] )
  end

  profile 'jruby.bash' do

    activation do
      file( :missing => '../bin/jruby' )
    end

    plugin :antrun do
      execute_goals( 'run',
                     :id => 'copy',
                     :phase => 'initialize',
                     'tasks' => {
                       'exec' => {
                         '@executable' =>  '/bin/sh',
                         '@osfamily' =>  'unix',
                         'arg' => {
                           '@line' =>  '-c \'cp "${jruby.basedir}/bin/jruby.bash" "${jruby.basedir}/bin/jruby"\''
                         }
                       },
                       'chmod' => {
                         '@file' =>  '${jruby.basedir}/bin/jruby',
                         '@perm' =>  '755'
                       }
                     } )
    end

  end

  profile 'native' do

    activation do
      file( :missing => '../lib/jni' )
    end

    plugin :dependency do
      execute_goals( 'unpack',
                     :id => 'unzip native',
                     :phase => 'process-classes',
                     'excludes' =>  'META-INF,META-INF/*',
                     'artifactItems' => [ { 'groupId' =>  'com.github.jnr',
                                            'artifactId' =>  'jffi',
                                            'version' =>  '${jffi.version}',
                                            'type' =>  'jar',
                                            'classifier' =>  'native',
                                            'overWrite' =>  'false',
                                            'outputDirectory' =>  '${jruby.basedir}/lib' } ] )
    end

  end

  profile 'test' do

    properties( 'maven.test.skip' => 'false' )

  end

  profile 'build.properties' do

    activation do
      file( :exits => '../build.properties' )
    end

    plugin 'org.codehaus.mojo:properties-maven-plugin:1.0-alpha-2' do
      execute_goals( 'read-project-properties',
                     :id => 'properties',
                     :phase => 'initialize',
                     'files' => [ '${jruby.basedir}/build.properties' ],
                     'quiet' =>  'true' )
    end

  end

  profile 'tzdata' do

    activation do
      property( :name => 'tzdata.version' )
    end

    properties( 'tzdata.jar.version' => '${tzdata.version}',
                'tzdata.scope' => 'runtime' )

  end
end
