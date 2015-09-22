version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Truffle OM DSL Processor' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-truffle-om-dsl-processor'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,

              'jruby.basedir' => '${basedir}/..' )

  jar 'org.jruby:jruby-truffle-om-dsl-api', '${project.version}', :scope => 'provided'

  truffle_version = '7a6719b66a744b822d9761cf8f2cd34904e22b2d-SNAPSHOT'
  jar 'com.oracle.truffle:truffle-api:' + truffle_version

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
end
