# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Digest

  class Base

    def update(message)
      Truffle::Digest.update @digest, message
    end

    alias_method :<<, :update

    def reset
      Truffle::Digest.reset @digest
    end

    def digest(message=nil)
      # TODO CS 18-May-15 need to properly handle missing argument, not pretend it's nil
      if message.nil?
        Truffle::Digest.digest @digest
      else
        reset
        update message
        digested = digest
        reset
        digested
      end
    end

  end

  class MD5 < Base

    def self.digest(message)
      digest = new
      digest.update message
      digest.digest
    end

    def initialize
      @digest = Truffle::Digest.md5
    end

  end

end
