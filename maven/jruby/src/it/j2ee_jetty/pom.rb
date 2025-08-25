# frozen_string_literal: true

# it is war-file
packaging 'war'

# default versions will be overwritten by pom.rb from root directory
properties("jruby.plugins.version": '3.0.6',
           "project.build.sourceEncoding": 'utf-8')

pom('org.jruby:jruby', '${jruby.version}')

# a gem to be used
gem 'flickraw', '0.9.7'

extension 'org.jruby.maven:mavengem-wagon:2.0.2'
repository id: :mavengems, url: 'mavengem:https://rubygems.org'

jruby_plugin :gem, includeRubygemsInResources: true, jrubyVersion: '9.0.0.0' do
  execute_goal :initialize
end

# start jetty for the tests
plugin('org.eclipse.jetty:jetty-maven-plugin', '9.1.3.v20140225',
       path: '/',
       stopPort: 9999,
       stopKey: 'foo') do
  execute_goal('start', id: 'start jetty', phase: 'pre-integration-test', daemon: true)
  execute_goal('stop', id: 'stop jetty', phase: 'post-integration-test')
end

# download files during the tests
result = nil
execute 'download', phase: 'integration-test' do
  require 'open-uri'
  result = open('http://localhost:8080').string
  puts result
end

# verify the downloads
execute 'check download', phase: :verify do
  expected = 'hello world:'
  raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)

  expected = 'uri:classloader:/gems/flickraw-0.9.7'
  raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)

  expected = 'snakeyaml-1.14.0'
  raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)
end
