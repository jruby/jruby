project 'JRuby Complete' do

  version = '9000.dev' #File.read( File.join( basedir, '..', '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'bundle'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  # the jar with classifier 'noasm' still has the dependencies
  # of the regular artifact and we need to exclude those which
  # are shaded into the 'noasm' artifact
  jar( 'org.jruby:jruby-core:${project.version}:noasm',
       :exclusions => [ 'com.github.jnr:jnr-ffi',
                        'org.ow2.asm:asm',
                        'org.ow2.asm:asm-commons',
                        'org.ow2.asm:asm-analysis',
                        'org.ow2.asm:asm-util' ] )
  jar 'org.jruby:jruby-stdlib:${project.version}'

  plugin( 'org.apache.felix:maven-bundle-plugin',
          :archive => {
            :manifest => {
              :mainClass => 'org.jruby.Main'
            }
          },
          :instructions => { 
            'Export-Package' => 'org.jruby.*;version=${project.version}',
            'Import-Package' => '!org.jruby.*, *;resolution:=optional',
            'Private-Package' => 'org.jruby.*,jnr.*,com.kenai.*,com.martiansoftware.*,jay.*,jline.*,jni.*,org.fusesource.*,org.jcodings.*,org.joda.convert.*,org.joda.time.*,org.joni.*,org.yaml.*,org.yecht.*,tables.*,org.objectweb.*,com.headius.*,org.bouncycastle.*,com.jcraft.jzlib,yaml.*,jruby.*,okay.*,.',
            'Bundle-Name' => 'JRuby ${project.version}',
            'Bundle-Description' => 'JRuby ${project.version} OSGi bundle',
            'Bundle-SymbolicName' => 'org.jruby.jruby',
            'Embed-Dependency' => '*;scope=compile|runtime;inline=true',
            'Embed-Transitive' => true
          } ) do
    extensions true
  end

  plugin( :invoker )

  # we have no sources and attach the sources and javadocs from jruby-core 
  # later in the build so IDE can use them

  plugin( :source, 'skipSource' =>  'true' )

  profile 'sonatype-oss-release' do

    # use the javadocs and sources from jruby-core !!!
    phase :package do
      plugin :dependency do
        items = [ 'sources', 'javadoc' ].collect do |classifier|
          { 'groupId' =>  '${project.groupId}',
            'artifactId' =>  'jruby-core',
            'version' =>  '${project.version}',
            'classifier' =>  classifier,
            'overWrite' =>  'false',
            'outputDirectory' =>  '${project.build.directory}' }
        end
        execute_goals( 'copy',
                       :id => 'copy javadocs and sources from jruby-core',
                       'artifactItems' => items )
      end
      
      plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
        execute_goals( 'attach-artifact',
                       :id => 'attach javadocs and sources artifacts',
                       'artifacts' => [ { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-sources.jar',
                                          'classifier' =>  'sources' },
                                        { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-javadoc.jar',
                                          'classifier' =>  'javadoc' } ] )
      end
    end
  end
end
