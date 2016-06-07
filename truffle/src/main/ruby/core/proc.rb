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

class Proc
  def self.__from_block__(env)
    # The compiler must be fixed before this method can be removed.
    Rubinius::Mirror::Proc.from_block self, env
  end

  attr_accessor :block
  attr_accessor :bound_method
  attr_accessor :ruby_method

  alias_method :===, :call

  def curry(curried_arity = nil)
    if lambda? && curried_arity
      if arity >= 0 && curried_arity != arity
        raise ArgumentError, "Wrong number of arguments (%i for %i)" % [
          curried_arity,
          arity
        ]
      end

      if arity < 0 && curried_arity < (-arity - 1)
        raise ArgumentError, "Wrong number of arguments (%i for %i)" % [
          curried_arity,
          -arity - 1
        ]
      end
    end

    args = []

    m = Rubinius::Mirror.reflect self
    f = m.curry self, [], arity

    f.singleton_class.send(:define_method, :binding) do
      raise ArgumentError, "cannot create binding from f proc"
    end

    f.singleton_class.thunk_method :parameters, [[:rest]]
    f.singleton_class.thunk_method :source_location, nil

    f
  end

  def to_s
    file, line = source_location

    l = " (lambda)" if lambda?
    if file and line
      "#<#{self.class}:0x#{self.object_id.to_s(16)}@#{file}:#{line}#{l}>"
    else
      "#<#{self.class}:0x#{self.object_id.to_s(16)}#{l}>"
    end
  end

  alias_method :inspect, :to_s

  def self.__from_method__(meth)
    obj = __allocate__
    obj.ruby_method = meth
    obj.lambda_style!

    return obj
  end

  def __yield__(*args, &block)
    @ruby_method.call(*args, &block)
  end


  def to_proc
    self
  end

  alias_method :[], :call
  alias_method :yield, :call

  def self.from_method(meth)
    if meth.kind_of? Method
      return __from_method__(meth)
    else
      raise ArgumentError, "tried to create a Proc object without a Method"
    end
  end
end
