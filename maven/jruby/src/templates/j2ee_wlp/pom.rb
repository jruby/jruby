# it is war-file
packaging 'war'

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '3.0.5',
            'project.build.sourceEncoding' => 'utf-8' )

pom( 'org.jruby:jruby', '${jruby.version}' )

# a gem to be used
gem 'virtus', '0.5.5'

extension 'org.jruby.maven:mavengem-wagon:2.0.2'
repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

jruby_plugin :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0' do
  execute_goal :initialize
end

execute 'deploy', :phase => 'pre-integration-test' do |ctx|
  wlp_home = ctx.basedir.to_pathname + '/../../wlp'
  FileUtils.cp( 'target/j2ee_wlp.war', "#{wlp_home}/usr/servers/testing/dropins/packed.war" )
  FileUtils.cp_r( 'target/j2ee_wlp', "#{wlp_home}/usr/servers/testing/dropins/unpacked.war" )
end

build do
  final_name '${project.artifactId}'
end

plugin( 'net.wasdev.wlp.maven.plugins:liberty-maven-plugin:1.0',
        :installDirectory => '${basedir}/../../wlp',
        :serverName => 'testing' ) do
  execute_goals( :'start-server',
                 :id => 'wlp-start',
                 :phase => 'pre-integration-test' )
  execute_goals( :'stop-server',
                 :id => 'wlp-stop',
                 :phase => 'post-integration-test' )
end

# download files during the tests
execute 'download', :phase => 'pre-integration-test' do
  require 'open-uri'
  result = open( 'http://localhost:9080/packed/index.jsp' ).string
  File.open( 'result1', 'w' ) { |f| f.puts result }
  result = open( 'http://localhost:9080/unpacked/index.jsp' ).string
  File.open( 'result2', 'w' ) { |f| f.puts result }
end

# verify the downloads
execute 'check download', :phase => :verify do
  [ 'result1', 'result2' ].each do |r|
    result = File.read( r )
    expected = 'hello world:'
    unless result.match( /#{expected}/ )
      raise "missed expected string in download: #{expected}"
    end
    expected = 'uri:classloader:/gems/backports-'
    unless result.match( /#{expected}/ )
      raise "missed expected string in download: #{expected}"
    end
  end
end
