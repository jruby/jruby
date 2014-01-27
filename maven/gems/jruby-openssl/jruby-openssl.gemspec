#-*- mode: ruby -*-

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
  s.files = Dir['[A-Z]*', 'lib/**/*', 'test/**/*'].to_a.reject { |f| f.match /_jars.rb$/ }

  s.add_development_dependency 'rake', "~> 10.1"
  s.add_development_dependency 'ruby-maven', "~> 3.1.1.0.0"

  s.requirements << "jar org.bouncycastle:bcpkix-jdk15on, 1.49"
  s.requirements << "jar org.bouncycastle:bcprov-jdk15on, 1.49"
end

# vim: syntax=Ruby
