require 'rake'
require 'rexml/document'
require 'rexml/xpath'
require_relative 'lib/jruby-jars/version'

Gem::Specification.new do |s|
  s.name = 'jruby-jars'
  s.version = JRubyJars::VERSION
  s.authors = ['Charles Oliver Nutter']
  s.email = 'headius@headius.com'
  s.summary = 'The core JRuby code and the JRuby stdlib as jar files.'
  s.homepage = 'http://github.com/jruby/jruby/tree/master/gem/jruby-jars'
  s.description = File.read('README.txt', encoding: 'UTF-8').split(/\n{2,}/)[3]
  s.rubyforge_project = 'jruby/jruby'
  s.licenses = %w(EPL-1.0 GPL-2.0 LGPL-2.1)
  s.files = FileList['[A-Z]*', 'lib/**/*.rb', "lib/**/jruby-*-#{JRubyJars::MAVEN_VERSION}.jar", 'test/**/*'].to_a
end
