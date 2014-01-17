project 'JRuby Main Maven Artifact' do

  version = '9000.dev' #File.read( File.join( basedir, '..', '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  # keep it a jar even without sources - easier to add in project
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby.home' => '${basedir}/../..',
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload

  plugin( :source, 'skipSource' =>  'true' )

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin( 'org.codehaus.mojo:build-helper-maven-plugin' )

  plugin( :invoker )

end
