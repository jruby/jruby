# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This is shimmed specifically for RubySpec's marshal specs

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
      end

      def encrypt
      end

      def decrypt
      end

      def random_iv
        "0123456789abcdefc633eebd46199f0255c9f49d"
      end

      def update(data)
        data
      end

      def final
        ''
      end

    end
  end

  module PKCS5
    def self.pbkdf2_hmac_sha1(secret, salt, iterations, key_size)
      "\xE4\xFF\xC3\xF0\xAE\x8F\x9Db\xCD\x9B\f_\x93o\xFD*\xFB\x0F\xE3\x935\xD5h7\xC0a\xA8\xA1\x9B\xB0\x03wj\xF5\xFA\xD6\x19:J \x80\x1A\xDF\xD0e\xAE1i\xFE\x10\xE6\xAFN\r\xD8`7\xA0\xA7\b\xDBk\x8C\xEF"
    end
  end

  module Digest
    %i[SHA SHA1 SHA224 SHA256 SHA384 SHA512].each do |name|
      const_set name, Class.new
    end
  end

  module HMAC
    def self.hexdigest(digest, key, data)
      "0123456789abcdefc633eebd46199f0255c9f49d"
    end
  end
end
