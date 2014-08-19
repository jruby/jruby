require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__),'..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Main Maven Artifact' do

  model_version '4.0.0'
  id "org.jruby:jruby:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom-generated.xml',
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  plugin( :source,
          'skipSource' =>  'true' )
  plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
    execute_goals( 'attach-artifact',
                   :id => 'attach-artifacts',
                   :phase => 'package',
                   'artifacts' => [ { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'sources' },
                                    { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'javadoc' } ] )
  end

  execute 'setup other osgi frameworks', :phase => 'pre-integration-test' do |ctx|
    felix = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_all_inclusive' )
     [ 'equinox-3.6', 'equinox-3.7', 'felix-3.2'].each do |m|
      target = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_all_inclusive_' + m )
      FileUtils.cp_r( felix, target )
      File.open( File.join( target, 'invoker.properties' ), 'w' ) do |f|
        f.puts 'invoker.profiles = ' + m
      end
    end
  end

  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh',
          'goals' => [ 'install' ] ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end

  profile :id => :jdk8 do
    activation do
      jdk '1.8'
    end
    plugin :invoker, :pomExcludes => ['osgi_all_inclusive_felix-3.2/pom.xml']
  end
  profile :id => :jdk6 do
    activation do
      jdk '1.6'
    end
    plugin :invoker, :pomExcludes => ['jetty/pom.xml','j2ee_jetty/pom.xml','j2ee_wildfly/pom.xml']
  end

  profile :id => :wlp do
    activation do
      property :name => 'wlp.jar'
    end
    execute :install_wlp, :phase => :'pre-integration-test' do |ctx|
      wlp = ctx.project.properties[ 'wlp.jar' ] || java.lang.System.properties[ 'wlp.jar' ]
      system( 'java -jar ' + wlp.to_pathname + ' --acceptLicense ' + ctx.project.build.directory.to_pathname )
      system( File.join( ctx.project.build.directory.to_pathname,
                         'wlp/bin/server' ) + 'create testing' )
      FileUtils.cp_r( File.join( ctx.basedir.to_pathname, 'src/templates/j2ee_wlp'), File.join( ctx.basedir.to_pathname, 'src/it' ) )
    end
  end
end
