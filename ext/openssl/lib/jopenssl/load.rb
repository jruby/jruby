unless defined? JRUBY_VERSION
  warn 'Loading jruby-openssl in a non-JRuby interpreter'
end

require 'jopenssl/version'
begin
  # if we have jar-dependencies we let it track the jars
  require 'jar-dependencies'
  require_jar( 'org.bouncycastle', 'bcpkix-jdk15on', Jopenssl::Version::BOUNCY_CASTLE_VERSION )
  require_jar( 'org.bouncycastle', 'bcprov-jdk15on', Jopenssl::Version::BOUNCY_CASTLE_VERSION )
rescue LoadError
  require "org/bouncycastle/bcpkix-jdk15on/#{Jopenssl::Version::BOUNCY_CASTLE_VERSION}/bcpkix-jdk15on-#{Jopenssl::Version::BOUNCY_CASTLE_VERSION}.jar"
  require "org/bouncycastle/bcprov-jdk15on/#{Jopenssl::Version::BOUNCY_CASTLE_VERSION}/bcprov-jdk15on-#{Jopenssl::Version::BOUNCY_CASTLE_VERSION}.jar"
end

# Load extension
require 'jruby'
require 'jopenssl.jar'
org.jruby.ext.openssl.OSSLLibrary.new.load(JRuby.runtime, false)

if RUBY_VERSION >= '2.1.0'
  load('jopenssl21/openssl.rb')
elsif RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl.rb')
else
  load('jopenssl18/openssl.rb')
end

require 'openssl/pkcs12'
