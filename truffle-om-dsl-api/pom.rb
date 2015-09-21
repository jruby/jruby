version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Truffle OM DSL API' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-truffle-om-dsl-api'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,

              'jruby.basedir' => '${basedir}/..' )

  jar 'org.jruby:jruby-core', '${project.version}', :scope => 'provided'

  jar 'com.oracle:truffle:0.7'

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
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-ea' ] )
  end

  plugin :shade do
    execute_goals( 'shade',
                   :id => 'create lib/jruby-truffle-om-dsl.jar',
                   :phase => 'package',
                   'outputFile' => '${jruby.basedir}/lib/jruby-truffle-om-dsl.jar' )
  end
end
