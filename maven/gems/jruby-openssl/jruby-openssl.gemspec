#-*- mode: ruby -*-

require 'rake'
require File.dirname(__FILE__) + "/lib/jopenssl/version.rb"

Gem::Specification.new do |s|
  s.name = 'jruby-openssl'
  s.version = Jopenssl::Version::VERSION
  s.authors = ['Ola Bini', 'JRuby contributors']
  s.email = "ola.bini@gmail.com"
  s.summary = "OpenSSL add-on for JRuby"
  s.homepage = 'https://github.com/jruby/jruby'
  s.description = File.read('README.txt').split(/\n{2,}/)[3...4].join("\n\n")
  s.rubyforge_project = 'jruby/jruby'
  s.files = FileList['[A-Z]*', 'lib/**/*', 'test/**/*'].to_a
  s.add_dependency('bouncy-castle-java', '>= 1.5.0147')

  s.add_development_dependency 'rake', "~> 10.1"
  s.add_development_dependency 'ruby-maven', "~> 3.1.0.0.0"

  # s.changes = File.read('History.txt').split(/\n{2,}/)[0..1].join("\n\n")
  s.require_paths = ['lib']
end

# vim: syntax=Ruby
