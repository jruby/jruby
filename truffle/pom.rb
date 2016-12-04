version = ENV['JRUBY_VERSION'] ||
  File.read( File.join( basedir, '..', 'VERSION' ) ).strip

project 'JRuby Truffle' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-truffle'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,
              # Must be the same as in mx.jruby/suite.py (except for the -SNAPSHOT part only in this file, and here we can use a release name)
              'truffle.version' => '0.20',
              'jruby.basedir' => '${basedir}/..',
              'maven.test.skip' => 'true',
              'tzdata.version' => '2013d',
              'tzdata.scope' => 'provided',
  )

  jar 'org.yaml:snakeyaml:1.14'

  if ENV['JRUBY_TRUFFLE_BUILD_STANDALONE']
    # exclude jnr-ffi to avoid problems with shading and relocation of the asm packages
    jar 'com.github.jnr:jnr-netdb:1.1.6', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-enxio:0.13', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-x86asm:1.0.2', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-unixsocket:0.14', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-posix:3.0.32', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-constants:0.9.6', :exclusions => ['com.github.jnr:jnr-ffi']
    jar 'com.github.jnr:jnr-ffi:2.1.1'
    jar 'com.github.jnr:jffi:${jffi.version}'
    jar 'com.github.jnr:jffi:${jffi.version}:native'

    jar 'org.jruby.joni:joni:2.1.11'
    jar 'org.jruby.extras:bytelist:1.0.13'
    jar 'org.jruby.jcodings:jcodings:1.0.18'

    jar 'bsf:bsf:2.4.0', :scope => 'provided'
    jar 'com.jcraft:jzlib:1.1.3'
    jar 'com.martiansoftware:nailgun-server:0.9.1'
    jar 'junit:junit', :scope => 'test'
    jar 'org.apache.ant:ant:${ant.version}', :scope => 'provided'

    # joda timezone must be before joda-time to be packed correctly
    jar 'org.jruby:joda-timezones:${tzdata.version}', :scope => '${tzdata.scope}'
    jar 'joda-time:joda-time:${joda.time.version}'
  else
    jar 'org.jruby:jruby-core', '${project.version}', :scope => 'provided'
  end

  repository(:url => 'http://lafo.ssw.uni-linz.ac.at/nexus/content/repositories/snapshots/', :id => 'truffle')

  truffle_version = '${truffle.version}'
  jar 'com.oracle.truffle:truffle-api:' + truffle_version
  jar 'com.oracle.truffle:truffle-debug:' + truffle_version
  jar 'com.oracle.truffle:truffle-dsl-processor:' + truffle_version, :scope => 'provided'
  jar 'com.oracle.truffle:truffle-tck:' + truffle_version, :scope => 'test'
  
  jar 'junit:junit', :scope => 'test'

  plugin( :compiler,
          'encoding' => 'utf-8',
          'debug' => 'true',
          'verbose' => 'false',
          'showWarnings' => 'true',
          'showDeprecation' => 'true',
          'source' => '1.8',
          'target' => '1.8',
          'useIncrementalCompilation' =>  'false' ) do
    execute_goals( 'compile',
                   :id => 'default-compile',
                   :phase => 'compile',
                   :fork => true,
                   'annotationProcessors' => [ 'com.oracle.truffle.object.dsl.processor.LayoutProcessor',
                                               'com.oracle.truffle.dsl.processor.InstrumentableProcessor',
                                               'com.oracle.truffle.dsl.processor.TruffleProcessor',
                                               'com.oracle.truffle.dsl.processor.interop.InteropDSLProcessor',
                                               'com.oracle.truffle.dsl.processor.verify.VerifyTruffleProcessor',
                                               'com.oracle.truffle.dsl.processor.LanguageRegistrationProcessor', ],
                   'generatedSourcesDirectory' =>  'target/generated-sources',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8',
                                       '-J-ea' ] )
  end

  plugin :shade do
    execute_goals( 'shade',
                   :id => 'create lib/jruby-truffle.jar',
                   :phase => 'package',
                   'outputFile' => '${jruby.basedir}/lib/jruby-truffle.jar' )
  end

  plugin( :surefire,
          'systemProperties' => {
              'jruby.home' =>  '${basedir}/..'
          },
          'additionalClasspathElements' => ['${basedir}'] )

  build do
    default_goal 'package'

    resource do
      directory 'src/main/ruby'
      includes '**/*rb'
      target_path '${project.build.directory}/classes/jruby-truffle'
    end

    resource do
      directory '${project.basedir}/..'
      includes [ 'BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY' ]
      target_path '${project.build.outputDirectory}/META-INF/'
    end
  end

  [ :dist, :'jruby-jars', :all, :release ].each do |name|
    profile name do
      plugin :shade do
        execute_goals( 'shade',
                       :id => 'pack jruby-truffle-complete.jar',
                       :phase => 'verify',
                       :artifactSet => { :includes => [
                          'com.oracle:truffle',
                          'com.oracle:truffle-interop' ] },
                       :outputFile => '${project.build.directory}/jruby-truffle-${project.version}-complete.jar' )
      end
    end
  end

  profile 'tck' do
    properties( 'maven.test.skip' => 'false' )
  end
end
