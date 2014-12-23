#-*- mode: ruby -*-

require 'fileutils'

version = File.read( File.join( File.dirname(File.expand_path(__FILE__)), '..', '..', 'VERSION' ) ).strip

# this regexp can be refined to work with pre, rc1, rc2 and such cases
ruby_version = version.sub( /-SNAPSHOT$/, '.dev' )

FileUtils.mkdir_p( 'lib/jruby-jars' )
File.open( 'lib/jruby-jars/version.rb', 'w' ) do |f|
  f.print <<EOF
module JRubyJars
  VERSION = '#{ruby_version}'
  MAVEN_VERSION = '#{version}'
end
EOF
end

Gem::Specification.new do |s|
  s.name = 'jruby-jars'
  s.version = ruby_version
  s.authors = ['Charles Oliver Nutter']
  s.email = 'headius@headius.com'
  s.summary = 'The core JRuby code and the JRuby stdlib as jar files.'
  s.homepage = 'http://github.com/jruby/jruby/tree/master/gem/jruby-jars'
  s.description = File.read('README.txt', encoding: 'UTF-8').split(/\n{2,}/)[3]
  s.rubyforge_project = 'jruby/jruby'
  s.licenses = %w(EPL-1.0 GPL-2.0 LGPL-2.1)
  s.files = Dir['[A-Z]*'] + Dir['lib/**/*.rb'] + Dir[ "lib/jruby-*-#{version}*.jar" ] + Dir[ 'test/**/*'] + [ 'jruby-jars.gemspec' ]
end

# vim: syntax=Ruby
