module JRuby
  module OpenSSL
    GEM_ONLY = false unless defined?(GEM_ONLY)
  end
end

if JRuby::OpenSSL::GEM_ONLY
  require 'jruby/openssl/gem'
else
  module OpenSSL
    class OpenSSLError < StandardError; end
    # These require the gem
    %w[
    ASN1
    BN
    Cipher
    Config
    Netscape
    PKCS7
    PKey
    Random
    SSL
    X509
    ].each {|c| autoload c, "jruby/openssl/gem"}
  end
  require "jruby/openssl/builtin"
end