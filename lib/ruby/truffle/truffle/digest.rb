# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# The part enclosed between START from MRI/END from MRI is licensed
# under LICENSE.RUBY as it is derived from lib/ruby/stdlib/digest.rb.

module Digest

  # START from MRI
  class ::Digest::Class
    # Creates a digest object and reads a given file, _name_.
    # Optional arguments are passed to the constructor of the digest
    # class.
    #
    #   p Digest::SHA256.file("X11R6.8.2-src.tar.bz2").hexdigest
    #   # => "f02e3c85572dc9ad7cb77c2a638e3be24cc1b5bea9fdbb0b0299c9668475c534"
    def self.file(name, *args)
      new(*args).file(name)
    end

    # Returns the base64 encoded hash value of a given _string_.  The
    # return value is properly padded with '=' and contains no line
    # feeds.
    def self.base64digest(str, *args)
      [digest(str, *args)].pack('m0')
    end
  end

  module Instance
    # Updates the digest with the contents of a given file _name_ and
    # returns self.
    def file(name)
      File.open(name, "rb") {|f|
        buf = ""
        while f.read(16384, buf)
          update buf
        end
      }
      self
    end

    # If none is given, returns the resulting hash value of the digest
    # in a base64 encoded form, keeping the digest's state.
    #
    # If a +string+ is given, returns the hash value for the given
    # +string+ in a base64 encoded form, resetting the digest to the
    # initial state before and after the process.
    #
    # In either case, the return value is properly padded with '=' and
    # contains no line feeds.
    def base64digest(str = nil)
      [str ? digest(str) : digest].pack('m0')
    end

    # Returns the resulting hash value and resets the digest to the
    # initial state.
    def base64digest!
      [digest!].pack('m0')
    end
  end
  # END from MRI

  NO_MESSAGE = Object.new

  def Digest.hexencode(message)
    StringValue(message).unpack('H*').first
  end

  module Instance
    def update(message)
      Truffle::Digest.update @digest, message
    end
    alias_method :<<, :update

    def reset
      Truffle::Digest.reset @digest
    end

    def digest(message = NO_MESSAGE)
      if NO_MESSAGE == message
        Truffle::Digest.digest @digest
      else
        reset
        update message
        digest!
      end
    end

    def hexdigest(message = NO_MESSAGE)
      Digest.hexencode(digest(message))
    end
    alias_method :to_s, :hexdigest
    alias_method :to_str, :hexdigest

    def digest!
      digested = digest
      reset
      digested
    end

    def hexdigest!
      digested = hexdigest
      reset
      digested
    end

    def digest_length
      Truffle::Digest.digest_length @digest
    end
    alias_method :size, :digest_length
    alias_method :length, :digest_length

    def ==(other)
      hexdigest == other.to_str
    end

    def inspect
      "#<#{self.class.name}: #{hexdigest}>"
    end
  end

  class Class
    include Instance

    def self.digest(message)
      digest = new
      digest.update message
      digest.digest
    end

    def self.hexdigest(message)
      digest = new
      digest.update message
      digest.hexdigest
    end
  end

  class Base < Class
  end

  class MD5 < Base
    def initialize
      @digest = Truffle::Digest.md5
    end

    def block_length
      64
    end
  end

  class SHA1 < Base
    def initialize
      @digest = Truffle::Digest.sha1
    end

    def block_length
      64
    end
  end

  class SHA256 < Base
    def initialize
      @digest = Truffle::Digest.sha256
    end

    def block_length
      64
    end
  end

  class SHA384 < Base
    def initialize
      @digest = Truffle::Digest.sha384
    end

    def block_length
      128
    end
  end

  class SHA512 < Base
    def initialize
      @digest = Truffle::Digest.sha512
    end

    def block_length
      128
    end
  end
end

require 'digest/sha2'
