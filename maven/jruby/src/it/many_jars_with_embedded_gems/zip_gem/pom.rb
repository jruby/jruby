#-*- mode: ruby -*-

VERSION = '2.0.2'

id 'rubygems:zip', VERSION

gem 'zip', VERSION

repository( :url => 'http://rubygems-proxy.torquebox.org/releases',
            :id => 'rubygems-releases' )

jruby_plugin! :gem, :includeRubygemsInResources => true
