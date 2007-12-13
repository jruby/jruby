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
    Random
    SSL
    ].each {|c| autoload c, "jruby/openssl/gem"}

  # These have fallbacks, but will still try to load the gem first
  %w[
    OPENSSL_VERSION
    OPENSSL_VERSION_NUMBER
    VERSION
    Digest
    DigestError
    HMAC
    HMACError
    ].each {|c| autoload c, "jruby/openssl/builtin"}

  # These are needed specifically because Rubygems itself references them
  %w[PKey X509].each {|c| autoload c, "jruby/openssl/rubygem_preloads"}
end
