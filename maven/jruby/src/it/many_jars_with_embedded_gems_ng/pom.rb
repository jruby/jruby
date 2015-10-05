#-*- mode: ruby -*-

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '1.0.10' )

packaging :pom

modules [ 'gem1', 'gem2', 'app' ]
