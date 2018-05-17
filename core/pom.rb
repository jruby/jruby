version = ENV['JRUBY_VERSION'] ||
  File.read( File.join( basedir, '..', 'VERSION' ) ).strip

project 'JRuby Core' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-core'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,

              'tzdata.version' => '2013d',
              'tzdata.scope' => 'provided',

              'maven.build.timestamp.format' => 'yyyy-MM-dd',
              'maven.test.skip' => 'true',
              'build.date' => '${maven.build.timestamp}',
              'main.basedir' => '${project.parent.basedir}',
              'Constants.java' => 'org/jruby/runtime/Constants.java',
              'anno.sources' => '${project.basedir}/target/generated-sources',

              'jruby.basedir' => '${basedir}/..',
              'jruby.test.memory' => '3G',
              'jruby.compile.memory' => '2G',

              'create.sources.jar' => false )

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

  # exclude jnr-ffi to avoid problems with shading and relocation of the asm packages
  jar 'com.github.jnr:jnr-netdb:1.1.6', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-enxio:0.17', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-x86asm:1.0.2', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-unixsocket:0.19', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-posix:3.0.45', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-constants:0.9.9', :exclusions => ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-ffi:2.1.8'
  jar 'com.github.jnr:jffi:${jffi.version}'
  jar 'com.github.jnr:jffi:${jffi.version}:native'

  jar 'org.jruby.joni:joni:2.1.16'
  jar 'org.jruby.extras:bytelist:1.0.15'
  jar 'org.jruby.jcodings:jcodings:1.0.30'
  jar 'org.jruby:dirgra:0.3'

  jar 'com.headius:invokebinder:1.11'
  jar 'com.headius:options:1.4'

  jar 'bsf:bsf:2.4.0', :scope => 'provided'
  jar 'com.jcraft:jzlib:1.1.3'
  jar 'com.martiansoftware:nailgun-server:0.9.1'
  jar 'junit:junit', :scope => 'test'
  jar 'org.apache.ant:ant:${ant.version}', :scope => 'provided'
  jar 'org.osgi:org.osgi.core:5.0.0', :scope => 'provided'

  # joda timezone must be before joda-time to be packed correctly
  jar 'org.jruby:joda-timezones:${tzdata.version}', :scope => '${tzdata.scope}'
  jar 'joda-time:joda-time:${joda.time.version}'

  # SLF4J only used within SLF4JLogger (JRuby logger impl) class
  jar 'org.slf4j:slf4j-api:1.7.12', :scope => 'provided', :optional => true
  jar 'org.slf4j:slf4j-simple:1.7.12', :scope => 'test'

  jar 'me.qmx.jitescript:jitescript:0.4.1', :exclusions => ['org.ow2.asm:asm-all']

  jar 'com.headius:modulator:1.0'

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
          'source' => [ '${base.java.version}', '1.8' ],
          'target' => [ '${base.javac.version}', '1.8' ],
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
                   'debug' => 'true',
                   'annotationProcessors' => [ 'org.jruby.anno.AnnotationBinder' ],
                   'generatedSourcesDirectory' =>  'target/generated-sources',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8',
                                       '-J-Xmx${jruby.compile.memory}' ] )
    execute_goals( 'compile',
                   :id => 'populators',
                   :phase => 'process-classes',
                   'debug' => 'true',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8',
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
                                     'includes' => [ '${Constants.java}' ] },
                                   { 'directory' =>  '${project.basedir}/..',
                                     'includes' => [ 'bin/jruby' ] },
                                   { 'directory' =>  '${project.basedir}/..',
                                     'includes' => [ 'lib/jni/**' ] } ],
                   'failOnError' =>  'false' )
  end

  plugin( :surefire,
          'forkCount' =>  '1',
          'reuseForks' =>  'false',
          'systemProperties' => {
            'jruby.home' =>  '${basedir}/..'
          },
          'argLine' =>  '-Xmx${jruby.test.memory} -Dfile.encoding=UTF-8 -Djava.awt.headless=true',
          'includes' => [ 'org/jruby/test/MainTestSuite.java',
                          'org/jruby/embed/**/*Test*.java',
                          'org/jruby/util/**/*Test*.java',
                          'org/jruby/runtime/**/*Test*.java' ],
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

    resource do
      directory '${project.basedir}/..'
      includes [ 'BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY' ]
      target_path '${project.build.outputDirectory}/META-INF/'
    end
  end


  plugin :shade do
    execute_goals( 'shade',
                   :id => 'create lib/jruby.jar',
                   :phase => 'package',
                   relocations: [
                       {pattern: 'org.objectweb', shadedPattern: 'org.jruby.org.objectweb' },
                   ],
                   'outputFile' => '${jruby.basedir}/lib/jruby.jar',
                   'transformers' => [ { '@implementation' => 'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                         'mainClass' => 'org.jruby.Main' } ],
                   'createSourcesJar' => '${create.sources.jar}',
                   filters:
                       {filter: {artifact: 'com.headius:invokebinder', excludes: {exclude: '**/module-info.class'}}}
    )
  end

  [:all, :release, :main, :osgi, :j2ee, :complete, :dist, :'jruby_complete_jar_extended', :'jruby-jars' ].each do |name|
    profile name do
      # we shade in all dependencies which use the asm classes and relocate
      # the asm package-name. with all jruby artifacts behave the same
      # regarding asm: lib/jruby, jruby-core and jruby-complete via maven
      plugin :shade do
        execute_goals( 'shade',
                       :id => 'shade the asm classes',
                       :phase => 'package',
                       'artifactSet' => {
                         # IMPORTANT these needs to match exclusions in
                         # maven/jruby-complete/pom.rb
                         'includes' => [ 'com.github.jnr:jnr-ffi',
                                         'me.qmx.jitescript:jitescript',
                                         'org.ow2.asm:*' ]
                       },
                       relocations: [
                           {pattern: 'org.objectweb', shadedPattern: 'org.jruby.org.objectweb' },
                           {pattern: 'me.qmx.jitescript', shadedPattern: 'org.jruby.me.qmx.jitescript'},
                       ],
                       filters:
                           {filter: {artifact: 'com.headius:invokebinder', excludes: {exclude: '**/module-info.class'}}}
        )
      end
    end
  end

  copy_goal = [:exec, :executable => '/bin/sh', :arguments => ['-c', 'cp ${jruby.basedir}/bin/jruby.bash ${jruby.basedir}/bin/jruby']]

  profile :clean do
    activation do
      # hack to get the os triggeer into the model
      os = org.apache.maven.model.ActivationOS.new
      os.family = 'unix'
      @current.os = os
    end

    phase :clean do
      plugin 'org.codehaus.mojo:exec-maven-plugin' do
        execute_goals( *copy_goal )
      end
    end
  end

  profile 'jruby.bash' do

    activation do
      file( :missing => '../bin/jruby' )
    end
    activation do
      # hack to get the os triggeer into the model
      os = org.apache.maven.model.ActivationOS.new
      os.family = 'unix'
      @current.os = os
    end

    phase :initialize do
      plugin 'org.codehaus.mojo:exec-maven-plugin' do
        execute_goals *copy_goal
      end
    end

  end

  jni_config = [ 'unpack', { :id => 'unzip native',
                             'excludes' =>  'META-INF,META-INF/*',
                             'artifactItems' => [ { 'groupId' =>  'com.github.jnr',
                                                    'artifactId' =>  'jffi',
                                                    'version' =>  '${jffi.version}',
                                                    'type' =>  'jar',
                                                    'classifier' =>  'native',
                                                    'overWrite' =>  'false',
                                                    'outputDirectory' =>  '${jruby.basedir}/lib' } ] } ]

  phase :clean do
    plugin :dependency do
      execute_goals( *jni_config  )
    end
  end

  profile 'native' do

    activation do
      file( :missing => '../lib/jni' )
    end

    phase 'process-classes' do
      plugin :dependency do
        execute_goals( *jni_config  )
      end
    end
  end

  profile 'test' do

    properties( 'maven.test.skip' => 'false' )

  end

  profile 'build.properties' do

    activation do
      file( :exists => '../build.properties' )
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

  profile 'generate sources jar' do
    activation do
      property( :name => 'create.sources.jar', :value => 'true' )
    end

    plugin :source do
      execute_goals( 'jar-no-fork',
                     :id => 'pack core sources',
                     :phase => 'prepare-package' ) # Needs to run before the shade plugin
    end
  end
end
