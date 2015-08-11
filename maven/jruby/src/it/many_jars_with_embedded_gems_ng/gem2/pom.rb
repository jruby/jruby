#-*- mode: ruby -*-

gemfile

model.repositories.clear
repository( :url => 'https://otto.takari.io/content/repositories/rubygems/maven/releases',
              :id => 'rubygems-releases' )

id 'org.rubygems:gem2', '2'

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
