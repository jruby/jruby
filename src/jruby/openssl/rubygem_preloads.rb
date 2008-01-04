# Rubygems expects these classes to be present after requiring openssl, so
# define them. If the gem is around it will update the definitions later
module OpenSSL
  module X509
    class Certificate; end
  end

  module PKey
    class PKeyError < OpenSSLError; end
    class PKey
      def initialize(arg)
      end
    end
    
    class RSAError < PKeyError; end
    class RSA < PKey; end
  end
  
  module Digest
    class Digest; end
    class SHA1 < Digest; end
  end
end

begin
  old_verbose, $VERBOSE = $VERBOSE, nil # silence warnings
  require 'jruby/openssl/gem' unless caller.detect {|tr| tr =~ /rubygems/}
rescue Exception
  $stderr.puts "The jruby-openssl gem is not installed; some operations may fail."
  $stderr.puts "Using builtin openssl, which only supports OpenSSL::HMAC and OpenSSL::Digest."
ensure
  $VERBOSE = old_verbose
end 
