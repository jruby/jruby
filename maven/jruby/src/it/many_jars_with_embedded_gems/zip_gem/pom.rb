#-*- mode: ruby -*-

gemfile

id 'org.rubygems:zip', VERSION

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
