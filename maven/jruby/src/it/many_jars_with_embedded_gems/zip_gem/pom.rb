#-*- mode: ruby -*-

gemfile

repositories.clear
repository( :url => 'https://otto.takari.io/content/repositories/rubygems/maven/releases',
              :id => 'rubygems-releases' )

id 'org.rubygems:zip', VERSION

jruby_plugin! :gem, :includeRubygemsInResources => true, :jrubyVersion => '9.0.0.0'
