#-*- mode: ruby -*-

gemfile

model.repositories.clear

extension 'org.torquebox.mojo:mavengem-wagon:1.0.3'
repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

id 'org.rubygems:zip', VERSION

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
