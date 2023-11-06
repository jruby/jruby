#-*- mode: ruby -*-

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '3.0.2' )

packaging :pom

modules [ 'gem1', 'gem2', 'app' ]
