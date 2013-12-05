# This file allows older version of JRuby (prior to 1.7.5) to explicitly load
# the gem version of jruby-openssl rather than the stdlib version. JRuby 1.7.5
# and higher use the "default gems" capability of RubyGems.

require 'jopenssl/load'