# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This is shimmed specifically for RubySpec's marshal specs, Rails, and ActiveSupport tests

require 'digest'

module OpenSSL
  module X509
    class Name

      def to_a
        raise "not implemented"
      end

    end
  end

  module Cipher
    CipherError = Class.new StandardError

    class Cipher
      attr_accessor :key, :iv

      def initialize(cipher)
        @encrypt = nil
        @iv      = nil
      end

      def encrypt
        @encrypt = true
      end

      def decrypt
        @encrypt = false
      end

      def random_iv
        @iv = (format('%10s', rand(16**10).to_s(16))*4).gsub(' ', '0')
      end

      def update(data)
        raise CipherError if @iv.nil?

        data.each_byte.each_with_index.map do |byte, i|
          iv_i    = i % 20
          iv_byte = @iv[(iv_i*2)..(iv_i*2+1)].to_i(16)
          (byte ^ iv_byte).chr
        end.join
      rescue => e
        # puts format "%s (%s)\n%s", e.message, e.class, e.backtrace.join("\n")
        raise CipherError
      end

      def final
        ''
      end

    end
  end

  module PKCS5
    def self.pbkdf2_hmac_sha1(secret, salt, iterations, key_size)
      (salt * (key_size / salt.size + 1))[0...key_size]
    end
  end

  module Digest
    %i[SHA1 SHA256 SHA384 SHA512].each do |name|
      klass = Class.new(::Digest.const_get(name)) do
        def name
          self.class.name
        end
      end

      const_set name, klass
    end
  end

  module HMAC
    def self.hexdigest(digest, key, data)
      # AS test data
      if data == "BAh7BjoIZm9vbzonTWVzc2FnZVZlcmlmaWVyVGVzdDo6QXV0b2xvYWRDbGFzcwY6CUBmb29JIghmb28GOgZFVA=="
        return "f3ef39a5241c365083770566dc7a9eb5d6ace914"
      end
      Digest::SHA1.hexdigest(data)
    end
  end

  module SSL
    SSLError = Class.new StandardError
  end

  class SSL::SSLSocket
  end
end
