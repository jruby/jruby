#-*- mode: ruby -*-

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '1.0.10' )

packaging :pom

modules [ 'zip_gem', 'app' ]
