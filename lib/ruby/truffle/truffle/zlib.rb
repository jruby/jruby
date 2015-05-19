# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Zlib

  DEFAULT_COMPRESSION = -1
  NO_COMPRESSION      = 0
  BEST_SPEED          = 1

  def self.crc32(*args)
    Truffle::Zlib.crc32(*args)
  end

  module Deflate

    def self.deflate(message, level)
      Truffle::Zlib.deflate(message, level)
    end

  end

  module Inflate

    def self.inflate(message)
      Truffle::Zlib.inflate(message)
    end

  end

end
