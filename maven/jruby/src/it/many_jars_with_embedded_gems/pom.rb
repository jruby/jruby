#-*- mode: ruby -*-

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '3.0.6-SNAPSHOT' )

packaging :pom

modules [ 'zip_gem', 'app' ]
