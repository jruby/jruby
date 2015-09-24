version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Truffle' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-truffle'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,

              'jruby.basedir' => '${basedir}/..' )

  jar 'org.yaml:snakeyaml:1.14'
  jar 'org.jruby:jruby-core', '${project.version}', :scope => 'provided'

  jar 'com.oracle:truffle:0.7'
  jar 'com.oracle:truffle-dsl-processor:0.7', :scope => 'provided'

  plugin( :compiler,
          'encoding' => 'utf-8',
          'debug' => 'true',
          'verbose' => 'false',
          'showWarnings' => 'true',
          'showDeprecation' => 'true',
          'source' => [ '${base.java.version}', '1.7' ],
          'target' => [ '${base.javac.version}', '1.7' ],
          'useIncrementalCompilation' =>  'false' ) do
    execute_goals( 'compile',
                   :id => 'anno',
                   :phase => 'process-resources',
                   'includes' => [ 'org/jruby/truffle/om/dsl/processor/OMProcessor.java' ],
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-ea' ] )
    execute_goals( 'compile',
                   :id => 'default-compile',
                   :phase => 'compile',
                   'annotationProcessors' => [ 'org.jruby.truffle.om.dsl.processor.OMProcessor',
                                               'com.oracle.truffle.dsl.processor.TruffleProcessor' ],
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

  build do
    default_goal 'package'

    resource do
      directory 'src/main/ruby'
      includes '**/*rb'
      target_path '${project.build.directory}/classes/jruby-truffle'
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
end
