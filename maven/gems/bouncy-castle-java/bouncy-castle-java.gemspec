#-*- mode: ruby -*-

require './bouncy-castle-version.rb'

Gem::Specification.new do |s|
  s.name = 'bouncy-castle-java'
  s.version = "1.5.0#{BouncyCastle::VERSION}"
  s.author = 'Hiroshi Nakamura'
  s.email = [ 'nahi@ruby-lang.org' ]
  s.rubyforge_project = "jruby-extras"
  s.homepage = 'http://github.com/jruby/jruby/tree/master/gems/bouncy-castle-java/'
  s.summary = 'Gem redistribution of Bouncy Castle jars'
  s.description = 'Gem redistribution of "Legion of the Bouncy Castle Java cryptography APIs" jars at http://www.bouncycastle.org/java.html'
  # TODO why ruby platform ???
  s.platform = Gem::Platform::RUBY
  s.files = ['README', 'LICENSE.html', 'lib/bouncy-castle-java.rb' ] + Dir['lib/bc*.jar' ]

  s.add_dependency 'ruby-maven', "=3.1.0.0.0"

  s.requirements << "jar org.bouncycastle:bcpkix-jdk15on, #{BouncyCastle::MAVEN_VERSION}"
  s.requirements << "jar org.bouncycastle:bcprov-jdk15on, #{BouncyCastle::MAVEN_VERSION}"
end

# vim: syntax=Ruby
