# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius

  L64 = true
  CPU = "jvm"
  SIZEOF_LONG = 8
  WORDSIZE = 8

  # Pretend to be Linux for the purposes of the FFI - doesn't make a difference anyway at this stage

  def self.windows?
    false
  end

  def self.darwin?
    false
  end

  def self.mathn_loaded?
    false
  end

  #def self.asm
    # No-op.
  #end

  class Fiber < ::Fiber

    ENABLED = true

    def initialize(size, &block)
      super(&block)
    end

  end

  module FFI
    class DynamicLibrary
    end
  end

  # jnr-posix hard codes this value
  PATH_MAX = 1024

  # Rubinius has a method for modifying attributes on global variables.  We handle that internally in JRuby+Truffle.
  # The shim API here is just to allow Rubinius code to run unmodified.
  class Globals
    def self.read_only(var)
      # No-op.
    end

    def self.set_hook(var)
      # No-op.
    end
  end

  class Backtrace
    def self.detect_backtrace(bt)
      false
    end
  end

end

class PrimitiveFailure < Exception
end
