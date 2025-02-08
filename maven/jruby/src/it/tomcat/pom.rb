id 'dummy:tomcat:1.0-SNAPSHOT'

# it is war-file
packaging 'war'

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '3.0.5',
            'project.build.sourceEncoding' => 'utf-8' )

pom( 'org.jruby:jruby', '${jruby.version}' )

# a gem to be used
gem 'flickraw', '0.9.7'

extension 'org.jruby.maven:mavengem-wagon:2.0.2'
repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

jruby_plugin :gem, :includeRubygemsInTestResources => false, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0' do
  execute_goal :initialize
end 

# start tomcat for the tests
plugin( 'org.codehaus.mojo:tomcat-maven-plugin', '1.1',
        :fork => true, :path => '/' ) do
  execute_goals( 'run',
                 :id => 'run-tomcat',
                 :phase => 'pre-integration-test' )
  execute_goals( 'shutdown',
                 :id => 'shutdown-tomcat',
                 :phase => 'post-integration-test' )
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
