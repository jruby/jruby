# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Symbol

  def to_sym
    self
  end

end

# Rubinius doesn't define Pointer#allocate, so we added a new primitive,
# :pointer_allocate, and define the method here. Needs to be before
# their primitive code is run as they create some instances during the
# class definition.

module Rubinius
  module FFI
    class Pointer

      def self.allocate
        Rubinius.primitive :pointer_allocate
        raise PrimitiveFailure, "FFI::Pointer.allocate primitive failed"
      end

    end
  end
end
