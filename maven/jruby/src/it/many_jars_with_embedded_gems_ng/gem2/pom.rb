#-*- mode: ruby -*-

gemfile

id 'org.rubygems:gem2', '2'

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
