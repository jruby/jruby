project 'JRuby Complete' do

  version = '9000.dev' #File.read( File.join( basedir, '..', '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  plugin :shade, '2.1' do
    execute_goals( 'shade',
                   :id => 'pack jruby.artifact',
                   :phase => 'package',
                   'relocations' => [ { 'pattern' =>  'org.objectweb',
                                        'shadedPattern' =>  'org.jruby.org.objectweb' } ],
                   'transformers' => [ { '@implementation' =>  'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                         'mainClass' =>  'org.jruby.Main' } ] )
  end

  plugin 'org.codehaus.mojo:exec-maven-plugin' do
    execute_goals( 'exec',
                   :id => 'unzip jruby-core.jar',
                   :phase => 'package',
                   'workingDirectory' =>  '${basedir}/../..',
                   'arguments' => [ '-d',
                                    '${project.build.outputDirectory}',
                                    '-o',
                                    '${project.build.directory}/${project.build.finalName}.jar' ],
                   'executable' =>  'unzip' )
  end

  plugin( 'org.apache.felix:maven-bundle-plugin',
          'archive' => {
            'manifest' => {
              'mainClass' =>  'org.jruby.Main'
            }
          } ) do
    execute_goals( 'manifest',
                   :phase => 'package' )
  end

  plugin :jar do
    execute_goals( 'jar',
                   :id => 'update manifest',
                   :phase => 'package',
                   'archive' => {
                     'manifestFile' =>  '${project.build.outputDirectory}/META-INF/MANIFEST.MF'
                   },
                   'excludes' => {
                     'exclue' =>  'Dummy.class'
                   } )
  end

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload

  plugin( :source, 'skipSource' =>  'true' )

  plugin( :invoker )

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
                       :id => 'copy gems',
                       'artifactItems' => items )
      end
      
      plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
        execute_goals( 'attach-artifact',
                       :id => 'attach-artifacts',
                       'artifacts' => [ { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-sources.jar',
                                          'classifier' =>  'sources' },
                                        { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-javadoc.jar',
                                          'classifier' =>  'javadoc' } ] )
      end
    end
  end
end
