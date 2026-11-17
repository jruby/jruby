# frozen_string_literal: true

# it is war-file
packaging 'war'

# default versions will be overwritten by pom.rb from root directory
properties("jruby.plugins.version": '3.0.6',
           "wildfly.version": '9.0.2.Final',
           "project.build.sourceEncoding": 'utf-8')

pom('org.jruby:jruby', '${jruby.version}')

# a gem to be used
gem 'virtus', '0.5.5'

extension 'org.jruby.maven:mavengem-wagon:2.0.2'
repository id: :mavengems, url: 'mavengem:https://rubygems.org'

jruby_plugin :gem, includeRubygemsInResources: true, jrubyVersion: '9.0.0.0' do
  execute_goal :initialize
end

plugin('org.wildfly.plugins:wildfly-maven-plugin:1.0.2.Final') do
  execute_goals(:start,
                id: 'wildfly-start',
                phase: 'pre-integration-test')
  execute_goals(:shutdown,
                id: 'wildfly-stop',
                phase: 'post-integration-test')
end

build do
  final_name '${project.artifactId}'
end

# download files during the tests
execute 'download', phase: 'integration-test' do
  require 'open-uri'
  dir = Dir['target/wildfly-run/*'].first
  FileUtils.cp('target/j2ee_wildfly.war', "#{dir}/standalone/deployments/packed.war")
  FileUtils.cp_r('target/j2ee_wildfly', "#{dir}/standalone/deployments/unpacked.war")
  FileUtils.touch("#{dir}/standalone/deployments/unpacked.war.dodeploy")

  # packed application
  count = 10
  begin
    sleep 1
    result = open('http://localhost:8080/packed/index.jsp').string
  rescue StandardError
    count -= 1
    retry if count.positive?
  end
  File.open('result1', 'w') { |f| f.puts result }

  # unpacked application
  count = 10
  begin
    sleep 1
    result = open('http://localhost:8080/unpacked/index.jsp').string
  rescue StandardError
    count -= 1
    retry if count.positive?
  end
  File.open('result2', 'w') { |f| f.puts result }
end

# verify the downloads
execute 'check download', phase: :verify do
  %w[result1 result2].each do |r|
    result = File.read(r)
    expected = 'hello world:'
    raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)

    expected = 'uri:classloader:/gems/backports-'
    raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)

    expected = 'snakeyaml-1.14.0'
    raise "missed expected string in download: #{expected}" unless result.match(/#{expected}/)
  end
end
