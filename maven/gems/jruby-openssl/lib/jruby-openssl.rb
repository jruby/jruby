# This file allows older version of JRuby (prior to 1.7.5) to explicitly load
# the gem version of jruby-openssl rather than the stdlib version. JRuby 1.7.5
# and higher use the "default gems" capability of RubyGems.

unless defined? JRUBY_VERSION
  warn 'Loading jruby-openssl in a non-JRuby interpreter'
end

# Load bouncy-castle gem if available
begin
  require 'bouncy-castle-java'
rescue LoadError
  # runs under restricted mode or uses builtin BC
end

# Load extension
require 'jruby'
require 'jopenssl.jar'
org.jruby.ext.openssl.OSSLLibrary.new.load(JRuby.runtime, false)

if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl.rb')
else
  load('jopenssl18/openssl.rb')
end

require 'openssl/pkcs12'
