id 'dummy:tomcat:1.0-SNAPSHOT'

packaging 'war'

gem 'flickraw', '0.9.7'

repository( 'http://rubygems-proxy.torquebox.org/releases',
            :id => 'rubygems-releases' )

# TODO switch jrubyVersion  to ${jruby.version} 
jruby_plugin :gem, :includeRubygemsInTestResources => false, :includeRubygemsInResources => true, :jrubyVersion => '1.7.10' do
  execute_goal :initialize
end 

properties( 'tesla.version' => '0.0.9',
            'jruby.version' => '9000.dev',
            'jruby.plugins.version' => '1.0.0-rc4',
            'gem.home' => '${project.build.outputDirectory}',
            'gem.path' => '${gem.home}',
            'project.build.sourceEncoding' => 'utf-8',
            'tesla.dump.pom' => 'pom.xml' )

jar( 'org.jruby:jruby:${jruby.version}',
     :exclusions => 'org.jruby:jruby-stdlib' )

# needed to install gems for the build itself
jar( 'org.jruby:jruby-stdlib', '${jruby.version}',
     :scope => :provided )

plugin( :dependency, '2.8',
        'artifactItems' => [ { 'groupId' =>  'org.jruby',
                               'artifactId' =>  'jruby-stdlib',
                               'version' =>  '${jruby.version}',
                               'outputDirectory' =>  '${project.build.outputDirectory}' } ] ) do
  execute_goals( 'unpack',
                 :phase => 'prepare-package' )
end

plugin( 'org.codehaus.mojo:tomcat-maven-plugin',
        :fork => true, :path => '/' ) do
  execute_goals( 'run',
                 :id => 'run-tomcat',
                 :phase => 'pre-integration-test' )
end


build do
  resource do
    directory 'src/main/ruby'
  end
end

result = nil
execute 'download', :phase => 'integration-test' do
  require 'open-uri'
  result = open( 'http://localhost:8080' ).string
end

execute 'check download', :phase => :verify do
  expected = 'hello world:'
  unless result.match( /#{expected}/ )
    raise "missed expected string in download: #{expected}"
  end
  expected = 'classes/gems/flickraw-0.9.7'
  unless result.match( /#{expected}/ )
    raise "missed expected string in download: #{expected}"
  end
end
