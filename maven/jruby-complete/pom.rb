require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__),'..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Complete' do

  model_version '4.0.0'
  id "org.jruby:jruby-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  plugin_repository( :id => 'rubygems-releases',
                     :url => 'http://rubygems-proxy.torquebox.org/releases' )

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readOnly' => true,
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar( 'org.jruby:jruby-stdlib:${project.version}',
       :exclusions => [ 'org.bouncycastle:bcpkix-jdk15on',
                        'org.bouncycastle:bcprov-jdk15on' ] )

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

  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh' ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end


  build do

    resource do
      directory '${jruby.basedir}/lib'
      includes '**/ruby/shared/bc*.jar'
      excludes 
      target_path '${jruby.complete.home}/lib'
    end
  end

  profile 'sonatype-oss-release' do

    plugin( :source,
            'skipSource' =>  'true' )
    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goals( 'attach-artifact',
                     :id => 'attach-artifacts',
                     :phase => 'package',
                     'artifacts' => [ { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-sources.jar',
                                        'classifier' =>  'sources' },
                                      { 'file' =>  '${project.build.directory}/jruby-core-${project.version}-javadoc.jar',
                                        'classifier' =>  'javadoc' } ] )
    end


    build do

      resource do
        directory '${jruby.basedir}/core/target'
        includes '*-sources.jar', '*-javadoc.jar'
        excludes 
        target_path '${project.build.directory}'
      end
    end

  end

end
