#-*- mode: ruby -*-

id 'org.jruby.test:osgi-complete:1'

packaging :pom

# default versions will be overwritten by pom.rb from root directory
properties( 'jruby.plugins.version' => '3.0.6-SNAPSHOT',
            'project.build.sourceEncoding' => 'utf-8' )

modules [ 'gems-bundle', 'scripts-bundle', 'test' ]
