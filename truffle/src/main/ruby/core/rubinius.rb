# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
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

# Only part of Rubinius' rubinius.rb

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

  module FFI
    class DynamicLibrary
    end
  end

  # jnr-posix hard codes this value
  PATH_MAX = 1024

  def self.watch_signal(sig, ignored)
    Truffle.primitive :vm_watch_signal
    raise PrimitiveFailure, "Rubinius.vm_watch_signal primitive failed"
  end

  def self.extended_modules(obj)
    Truffle.primitive :vm_extended_modules
    raise PrimitiveFailure, "Rubinius.extended_modules primitive failed"
  end

  def self.raise_exception(exc)
    Truffle.primitive :vm_raise_exception
    raise PrimitiveFailure, "Rubinius.vm_raise_exception primitive failed"
  end

  def self.throw(dest, obj)
    Truffle.primitive :vm_throw
    raise PrimitiveFailure, "Rubinius.throw primitive failed"
  end

  def self.catch(dest, obj)
    Truffle.primitive :vm_catch
    raise PrimitiveFailure, "Rubinius.catch primitive failed"
  end

  module Unsafe
    def self.set_class(obj, cls)
      Truffle.primitive :vm_set_class

      if obj.kind_of? ImmediateValue
        raise TypeError, "Can not change the class of an immediate"
      end

      raise ArgumentError, "Class #{cls} is not compatible with #{obj.inspect}"
    end
  end

  def self.get_user_home(name)
    Truffle.primitive :vm_get_user_home
    raise PrimitiveFailure, "Rubinius.get_user_home primitive failed"
  end
  
  def self.synchronize(object, &block)
    Truffle::System.synchronized(object, &block)
  end

  module Metrics
    def self.data
      {
          :'gc.young.count' => 0,
          :'gc.young.ms' => 0,
          :'gc.immix.concurrent.ms' => 0,
          :'gc.immix.count' => Truffle::GC.count,
          :'gc.immix.stop.ms' => Truffle::GC.time,
          :'gc.large.sweep.us' => 0,
          :'memory.young.bytes' => 0,
          :'memory.young.objects' => 0,
          :'memory.immix.bytes' => 0,
          :'memory.immix.objects' => 0,
          :'memory.large.bytes' => 0,
          :'memory.promoted.bytes' => 0,
          :'memory.promoted.objects' => 0,
          :'memory.symbols.bytes' => 0,
          :'memory.code.bytes' => 0
      }
    end
  end
end

class PrimitiveFailure < Exception
end
