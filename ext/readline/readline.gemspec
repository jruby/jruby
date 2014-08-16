#-*- mode: ruby -*-

require File.dirname(__FILE__) + "/lib/readline/version.rb"

Gem::Specification.new do |s|
  s.name = 'jruby-readline'
  s.version = Readline::Version::VERSION
  s.authors = [ 'JRuby contributors']
  s.email = "dev@jruby.org"
  s.summary = "JRuby Readline"
  s.homepage = 'https://github.com/jruby/jruby'
  s.description = "readline extension for JRuby"
  s.files = Dir['[A-Z]*'] + Dir['lib/**/*']
  s.platform = 'java'

  s.requirements << "jar jline:jline, #{Readline::Version::JLINE_VERSION}"
end

# vim: syntax=Ruby
