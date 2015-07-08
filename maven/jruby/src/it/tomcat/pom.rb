id 'dummy:tomcat:1.0-SNAPSHOT'

# it is war-file
packaging 'war'

# get jruby dependencies
properties( 'jruby.version' => '@project.version@',
            'project.build.sourceEncoding' => 'utf-8' )

pom( 'org.jruby:jruby', '${jruby.version}' )

# a gem to be used
gem 'flickraw', '0.9.7'

repository( :url => 'http://rubygems-proxy.torquebox.org/releases',
            :id => 'rubygems-releases' )

jruby_plugin :gem, :includeRubygemsInTestResources => false, :includeRubygemsInResources => true do
  execute_goal :initialize
end 

# ruby-maven will dump an equivalent pom.xml
properties[ 'tesla.dump.pom' ] = 'pom.xml'

# start tomcat for the tests
plugin( 'org.codehaus.mojo:tomcat-maven-plugin', '1.1',
        :fork => true, :path => '/' ) do
  execute_goals( 'run',
                 :id => 'run-tomcat',
                 :phase => 'pre-integration-test' )
end

# download files during the tests
result = nil
execute 'download', :phase => 'integration-test' do
  require 'open-uri'
  result = open( 'http://localhost:8080' ).string
  puts result
end

# verify the downloads
execute 'check download', :phase => :verify do
  expected = 'hello world:'
  unless result.match( /#{expected}/ )
    raise "missed expected string in download: #{expected}"
  end
  expected = 'uri:classloader:/gems/flickraw-0.9.7'
  unless result.match( /#{expected}/ )
    raise "missed expected string in download: #{expected}"
  end
end
