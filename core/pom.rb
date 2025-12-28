# frozen_string_literal: true

version = ENV['JRUBY_VERSION'] ||
          File.read(File.join(basedir, '..', 'VERSION')).strip

# NOTE: we keep the legacy name since a lot of tests depends on it but
#       rename artifact
project 'JRuby Base' do
  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-base'

  properties("polyglot.dump.pom": 'pom.xml',
             "polyglot.dump.readonly": true,

             "tzdata.version": '2019c',
             "tzdata.scope": 'provided',

             "maven.build.timestamp.format": 'yyyy-MM-dd',
             "maven.test.skip": 'true',
             "build.date": '${maven.build.timestamp}',
             "main.basedir": '${project.parent.basedir}',
             "Constants.java": 'org/jruby/runtime/Constants.java',
             "anno.sources": '${project.basedir}/target/generated-sources',

             "jruby.basedir": '${basedir}/..',
             "jruby.test.memory": '3G',
             "jruby.compile.memory": '2G',

             "create.sources.jar": false)

  IO.foreach(File.join(basedir, '..', 'default.build.properties')) do |line|
    line.chomp!
    # skip comments
    next if line =~ /(^\W*#|^$)/

    # build const name
    name, value = line.split('=', 2)
    properties name => value
  end

  jar 'org.ow2.asm:asm:${asm.version}'
  jar 'org.ow2.asm:asm-commons:${asm.version}'
  jar 'org.ow2.asm:asm-util:${asm.version}'

  # exclude jnr-ffi to avoid problems with shading and relocation of the asm packages
  jar 'com.github.jnr:jnr-netdb:1.2.0', exclusions: ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-enxio:0.32.19', exclusions: ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-unixsocket:0.38.24', exclusions: ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-posix:3.1.21', exclusions: ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-constants:0.10.4', exclusions: ['com.github.jnr:jnr-ffi']
  jar 'com.github.jnr:jnr-ffi:2.2.18'
  jar 'com.github.jnr:jffi:${jffi.version}'
  jar 'com.github.jnr:jffi:${jffi.version}:native'

  jar 'org.jruby.joni:joni:2.2.6'
  jar 'org.jruby.jcodings:jcodings:1.0.63'
  jar 'org.jruby:dirgra:0.5'

  jar 'com.headius:invokebinder:1.14'
  jar 'com.headius:options:1.6'

  jar 'org.jruby:jzlib:1.1.5'
  jar 'junit:junit', scope: 'test'
  jar 'org.awaitility:awaitility', scope: 'test'
  jar 'org.apache.ant:ant:${ant.version}', scope: 'provided'
  jar 'org.osgi:org.osgi.core:5.0.0', scope: 'provided'

  # joda timezone must be before joda-time to be packed correctly
  # jar 'org.jruby:joda-timezones:${tzdata.version}', :scope => '${tzdata.scope}'
  jar 'joda-time:joda-time:${joda.time.version}'

  # SLF4J only used within SLF4JLogger (JRuby logger impl) class
  jar 'org.slf4j:slf4j-api:1.7.12', scope: 'provided', optional: true
  jar 'org.slf4j:slf4j-simple:1.7.12', scope: 'test'

  jar 'me.qmx.jitescript:jitescript:0.4.1', exclusions: ['org.ow2.asm:asm-all']

  jar 'com.headius:backport9:1.13'

  jar 'org.crac:crac:1.5.0'

  jar 'ch.randelshofer:fastdoubleparser:2.0.1'

  plugin_management do
    plugin('org.eclipse.m2e:lifecycle-mapping:1.0.0',
           lifecycleMappingMetadata: {
             pluginExecutions: [{ pluginExecutionFilter: {
                                    groupId: 'org.codehaus.mojo',
                                    artifactId: 'properties-maven-plugin',
                                    versionRange: '[1.0-alpha-2,)',
                                    goals: ['read-project-properties']
                                  },
                                  action: {
                                    ignore: ''
                                  } },
                                { pluginExecutionFilter: {
                                    groupId: 'org.codehaus.mojo',
                                    artifactId: 'build-helper-maven-plugin',
                                    versionRange: '[1.8,)',
                                    goals: ['add-source']
                                  },
                                  action: {
                                    ignore: ''
                                  } },
                                { pluginExecutionFilter: {
                                    groupId: 'org.codehaus.mojo',
                                    artifactId: 'exec-maven-plugin',
                                    versionRange: '[1.2.1,)',
                                    goals: ['exec']
                                  },
                                  action: {
                                    ignore: ''
                                  } },
                                { pluginExecutionFilter: {
                                    groupId: 'org.apache.maven.plugins',
                                    artifactId: 'maven-dependency-plugin',
                                    versionRange: '[2.8,)',
                                    goals: ['copy']
                                  },
                                  action: {
                                    ignore: ''
                                  } },
                                { pluginExecutionFilter: {
                                    groupId: 'org.apache.maven.plugins',
                                    artifactId: 'maven-clean-plugin',
                                    versionRange: '[2.5,)',
                                    goals: ['clean']
                                  },
                                  action: {
                                    ignore: ''
                                  } }]
           })
  end

  plugin 'org.codehaus.mojo:buildnumber-maven-plugin:1.2' do
    execute_goals('create',
                  id: 'jruby-revision',
                  phase: 'generate-sources',
                  buildNumberPropertyName: 'jruby.revision')
  end

  phase 'process-classes' do
    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goals('add-source',
                    id: 'add-populators',
                    sources: ['${anno.sources}'])
    end

    plugin 'org.codehaus.mojo:exec-maven-plugin' do
      execute_goals('exec',
                    id: 'invoker-generator',
                    arguments: ['-Djruby.bytecode.version=${base.java.version}',
                                '-classpath',
                                xml('<classpath/>'),
                                'org.jruby.anno.InvokerGenerator',
                                '${anno.sources}/annotated_classes.txt',
                                '${project.build.outputDirectory}'],
                    executable: 'java',
                    classpathScope: 'compile')

      execute_goals('exec',
                    id: 'scope-generator',
                    arguments: ['-Djruby.bytecode.version=${base.java.version}',
                                '-classpath',
                                xml('<classpath/>'),
                                'org.jruby.runtime.scope.DynamicScopeGenerator',
                                '${project.build.outputDirectory}'],
                    executable: 'java',
                    classpathScope: 'compile')

      execute_goals('exec',
                    id: 'specialized-object-generator',
                    arguments: ['-Djruby.bytecode.version=${base.java.version}',
                                '-classpath',
                                xml('<classpath/>'),
                                'org.jruby.specialized.RubyObjectSpecializer',
                                '${project.build.outputDirectory}'],
                    executable: 'java',
                    classpathScope: 'compile')
    end
  end

  fork_compiler_args = ['-XDignore.symbol.file=true',
                        '-J-Duser.language=en',
                        '-J-Dfile.encoding=UTF-8',
                        '-J-Xmx${jruby.compile.memory}']

  default_compile_configuration = {
    fork: 'true',
    annotationProcessors: ['org.jruby.anno.AnnotationBinder'],
    generatedSourcesDirectory: 'target/generated-sources',
    compilerArgs: fork_compiler_args
  }

  plugin(:compiler,
         encoding: 'utf-8',
         verbose: 'false',
         showWarnings: 'true',
         showDeprecation: 'true',
         source: ['${base.java.version}', '21'],
         target: ['${base.javac.version}', '21'],
         useIncrementalCompilation: 'false') do
    execute_goals('compile',
                  id: 'anno',
                  phase: 'process-resources',
                  includes: ['org/jruby/anno/FrameField.java',
                             'org/jruby/anno/AnnotationBinder.java',
                             'org/jruby/anno/JRubyMethod.java',
                             'org/jruby/anno/FrameField.java',
                             'org/jruby/runtime/Visibility.java',
                             'org/jruby/util/CodegenUtils.java',
                             'org/jruby/util/SafePropertyAccessor.java'])
    execute_goals('compile',
                  default_compile_configuration.merge(
                    id: 'default-compile',
                    phase: 'compile'
                  ))

    execute_goals('compile',
                  id: 'populators',
                  phase: 'process-classes',
                  debug: 'false',
                  fork: 'true',
                  compilerArgs: fork_compiler_args,
                  includes: ['org/jruby/gen/**/*.java'])
  end

  plugin :clean do
    execute_goals('clean',
                  id: 'default-clean',
                  phase: 'clean',
                  filesets: [{ directory: '${project.build.sourceDirectory}',
                               includes: ['${Constants.java}'] },
                             { directory: '${project.basedir}/..',
                               includes: ['lib/jni/**'] }],
                  failOnError: 'false')
  end

  plugin(:surefire,
         :forkCount => '1',
         :reuseForks => 'false',
         :systemProperties => {
           "jruby.home": '${basedir}/..'
         },
         :argLine => '-Xmx${jruby.test.memory} -Dfile.encoding=UTF-8 -Djava.awt.headless=true',
         # rubocop:disable Style/StringHashKeys
         'environmentVariables' => {
           'JDK_JAVA_OPTIONS' => '--add-modules java.scripting --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED'
         },
         includes: [
           'org/jruby/test/**/*Test*.java',
           'org/jruby/embed/**/*Test*.java',
           'org/jruby/exceptions/**/*Test*.java',
           'org/jruby/ext/**/*Test*.java',
           'org/jruby/java/**/*Test*.java',
           'org/jruby/javasupport/*Test*.java',
           'org/jruby/util/**/*Test*.java',
           'org/jruby/runtime/**/*Test*.java'
         ],
         'additionalClasspathElements' => ['${basedir}/src/test/ruby'])

  plugin(:jar,
         archive: {
           manifestEntries: {
             # Module name for JRuby
             'Automatic-Module-Name' => 'org.jruby.base',

             # Open requested packages when not running as a module (i.e. JRuby is on classpath or -jar main jar)
             'Add-Opens' => 'java.base/java.io java.base/java.nio.channels java.base/sun.nio.ch java.management/sun.management',

             # Enable native access for JRuby and classpath classes when run not as a module
             'Enable-Native-Access' => 'ALL-UNNAMED',
           }
         })

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
  end

  plugin :resources do
    execute_goals('copy-resources', phase: 'process-resources',
                                    outputDirectory: '${basedir}',
                                    resources: [
                                      {
                                        directory: 'src/main/resources',
                                        includes: '${Constants.java}',
                                        target_path: '${project.build.sourceDirectory}',
                                        filtering: 'true'
                                      },
                                      {
                                        directory: '..',
                                        includes: ['BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY', 'VERSION'],
                                        target_path: '${project.build.sourceDirectory}/META-INF/'
                                      }
                                    ])
  end

  profile 'error-prone' do
    activation do
      jdk('21')
      property(name: 'env.CI') # for keeping fast development cycle, by default only run on CI
    end

    plugin :compiler do
      execute_goals('compile',
                    id: 'default-compile',
                    phase: 'none') # do not execute default-compile, we have a replacement bellow

      extra_compiler_args = %w(
        -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
        -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        -J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
        -J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
      )

      execute_goals('compile',
                    default_compile_configuration.merge(
                      id: 'default-compile_with_error_prone',
                      phase: 'compile',
                      'compilerArgs' => default_compile_configuration[:compilerArgs] + [
                        '-XDcompilePolicy=simple', '--should-stop=ifError=FLOW', '-Xplugin:ErrorProne'
                      ] + extra_compiler_args,
                      'annotationProcessorPaths' => {
                        'path' => [{
                                     'groupId' => 'com.google.errorprone',
                                     'artifactId' => 'error_prone_core',
                                     'version' => '2.39.0'
                                   },
                                   {
                                     'groupId' => 'org.jruby',
                                     'artifactId' => 'jruby-base',
                                     'version' => version
                                    }]
                      }
                    ))
    end
  end

  jni_config = ['unpack', { :id => 'unzip native',
                            'excludes' => 'META-INF,META-INF/*',
                            'artifactItems' => [{ 'groupId' => 'com.github.jnr',
                                                  'artifactId' => 'jffi',
                                                  'version' => '${jffi.version}',
                                                  'type' => 'jar',
                                                  'classifier' => 'native',
                                                  'overWrite' => 'false',
                                                  'outputDirectory' => '${jruby.basedir}/lib' }] }]

  phase :clean do
    plugin :dependency do
      execute_goals(*jni_config)
    end
  end

  profile 'native' do
    activation do
      file(missing: '../lib/jni')
    end

    phase 'process-classes' do
      plugin :dependency do
        execute_goals(*jni_config)
      end
    end
  end

  profile 'test' do
    properties('maven.test.skip' => 'false')
  end

  profile 'build.properties' do
    activation do
      file(exists: '../build.properties')
    end

    plugin 'org.codehaus.mojo:properties-maven-plugin:1.0-alpha-2' do
      execute_goals('read-project-properties',
                    :id => 'properties',
                    :phase => 'initialize',
                    'files' => ['${jruby.basedir}/build.properties'],
                    'quiet' => 'true')
    end
  end

  profile 'tzdata' do
    activation do
      property(name: 'tzdata.version')
    end

    properties('tzdata.jar.version' => '${tzdata.version}',
               'tzdata.scope' => 'runtime')
  end

  profile 'generate sources jar' do
    activation do
      property(name: 'create.sources.jar', value: 'true')
    end

    plugin :source do
      execute_goals('jar-no-fork',
                    id: 'pack core sources',
                    phase: 'prepare-package') # Needs to run before the shade plugin
    end
  end
end
