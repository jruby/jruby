project 'JRuby Rake Plugin' do

  version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby.plugins:jruby-rake-plugin:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'maven-plugin'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar 'org.apache.maven:maven-plugin-api:2.2.1'
  jar 'org.apache.maven:maven-project:2.2.1'
  jar 'ant:ant:1.6.5'
  jar 'org.jruby:jruby-core:${project.version}'

  plugin( :plugin,
          'goalPrefix' =>  'jruby-rake' )
  plugin( :compiler,
          'excludes' => [ 'none' ] )
  plugin( :jar,
          'excludes' => [ 'none' ] )

  build do
    output_directory 'target/classes'
    source_directory 'src/main/java'
  end

end
