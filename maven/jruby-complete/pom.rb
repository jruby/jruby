require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__),'..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Complete' do

  model_version '4.0.0'
  id "org.jruby:jruby-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'bundle'

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

  plugin( 'org.apache.felix:maven-bundle-plugin',
          'archive' => {
            'manifest' => {
              'mainClass' =>  'org.jruby.Main'
            }
          },
          :instructions => { 
            'Export-Package' => 'org.jruby.*;version=${project.version}',
            'Import-Package' => '!org.jruby.*, *;resolution:=optional',
            'DynamicImport-Package' => '*',
            'Embed-Dependency' => '*;type=jar;scope=compile|runtime;inline=true',
            'Embed-Transitive' => true,
            'Private-Package' => '*,.',
            'Bundle-Name' => 'JRuby ${project.version}',
            'Bundle-Description' => 'JRuby ${project.version} OSGi bundle',
            'Bundle-SymbolicName' => 'org.jruby.jruby'
          } ) do
    # TODO fix DSL
    @current.extensions = true
  end

  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh',
          'goals' => ['install'] ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end

  execute 'setup other osgi frameworks', :phase => 'pre-integration-test' do |ctx|
    require 'fileutils'
    felix = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_many_bundles_with_embedded_gems' )
    [ 'equinox-3.6', 'equinox-3.7', 'felix-3.2' ].each do |m|
      target = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_many_bundles_with_embedded_gems_' + m )
      FileUtils.cp_r( felix, target )
      File.open( File.join( target, 'invoker.properties' ), 'w' ) do |f|
        f.puts 'invoker.profiles = ' + m
      end
    end
  end

  build do

    resource do
      directory '${jruby.basedir}/lib'
      includes '**/ruby/shared/bc*.jar'
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
        target_path '${project.build.directory}'
      end
    end

  end

  profile :id => :jdk8 do
    activation do
      jdk '1.8'
    end
    plugin :invoker, :pomExcludes => ['osgi_many_bundles_with_embedded_gems_felix-3.2/pom.xml']
  end
end
