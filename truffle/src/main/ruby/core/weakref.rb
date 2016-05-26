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

class WeakRef < BasicObject

  class RefError < ::RuntimeError; end

  def self.new(obj)
    Truffle.primitive :weakref_new
    ::Kernel.raise PrimitiveFailure, "WeakRef.new primitive failed"
  end

  def __setobj__(obj)
    Truffle.primitive :weakref_set_object
    ::Kernel.raise PrimitiveFailure, "WeakRef#__setobj__ primitive failed"
  end

  def __object__
    Truffle.primitive :weakref_object
    ::Kernel.raise PrimitiveFailure, "WeakRef#__object__ primitive failed"
  end

  def __getobj__
    obj = __object__()
    ::Kernel.raise RefError, "Object has been collected as garbage" unless obj
    return obj
  end

  def weakref_alive?
    !!__object__
  end

  def method_missing(method, *args, &block)
    target = __getobj__
    if target.respond_to?(method)
      target.__send__(method, *args, &block)
    else
      super(method, *args, &block)
    end
  end

  def respond_to_missing?(method, include_private)
    target = __getobj__
    target.respond_to?(method, include_private) and
      (!include_private || target.respond_to?(method, false))
  end
end
