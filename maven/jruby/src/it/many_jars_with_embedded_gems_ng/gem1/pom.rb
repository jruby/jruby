#-*- mode: ruby -*-

gemfile

id 'org.rubygems:gem1', '1'

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
