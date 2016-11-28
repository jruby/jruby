# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015 Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Module

  # :internal:
  #
  # Basic version of .include used in kernel code.
  #
  # Redefined in kernel/delta/module.rb.
  #
  def include(mod)
    Truffle.privately do
      mod.append_features self
      mod.included self
    end
    self
  end

end

module Kernel

  # Rubinius defines this method differently, using the :object_class primitive.  The two primitives are very similar,
  # so rather than introduce the new one, we'll just delegate to the existing one.
  def __class__
    Truffle.invoke_primitive :vm_object_class, self
  end

  alias_method :eql?, :equal?

  alias_method :send, :__send__ # from BasicObject

end

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
        Truffle.primitive :pointer_allocate
        raise PrimitiveFailure, "FFI::Pointer.allocate primitive failed"
      end

    end
  end
end
