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

  Truffle.omit("Rubinius-specific initialization.") do
    def self.new(*args)
      env = nil

      Rubinius.asm do
        push_block
        # assign a pushed block to the above local variable "env"
        # Note that "env" is indexed at 1, not 0. "args" is indexed at 0.
        set_local 1
      end

      unless env
        # Support for ancient pre-block-pass style:
        # def something; Proc.new; end
        # something { a_block } => Proc instance
        env = Rubinius::BlockEnvironment.of_sender

        unless env
          raise ArgumentError, "tried to create a Proc object without a block"
        end
      end

      block = Rubinius::Mirror::Proc.from_block self, env

      if block.class != self
        block = block.dup
        Rubinius::Unsafe.set_class(block, self)
      end

      Rubinius.asm(block, args) do |b, a|
        run b
        run a
        run b
        send_with_splat :initialize, 0, true
      end

      return block
    end
  end

  attr_accessor :block
  attr_accessor :bound_method
  attr_accessor :ruby_method

  def binding
    bind = @block.to_binding
    bind.proc_environment = @block
    bind
  end

  def arity
    if @ruby_method
      return @ruby_method.arity
    elsif @bound_method
      arity = @bound_method.arity
      return arity < 0 ? -1 : arity
    end

    @block.arity
  end

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

  def source_location
    if @ruby_method
      @ruby_method.source_location
    elsif @bound_method
      if @bound_method.respond_to?(:source_location)
        @bound_method.source_location
      else
        nil
      end
    else
      @block.source_location
    end
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

  def parameters
    if @ruby_method
      return @ruby_method.parameters
    elsif @bound_method
      return @bound_method.parameters
    end

    code = @block.compiled_code

    params = []

    return params unless code.respond_to? :local_names

    m = code.required_args - code.post_args
    o = m + code.total_args - code.required_args
    p = o + code.post_args
    p += 1 if code.splat

    required_status = self.lambda? ? :req : :opt

    code.local_names.each_with_index do |name, i|
      if i < m
        params << [required_status, name]
      elsif i < o
        params << [:opt, name]
      elsif code.splat == i
        if name == :*
          params << [:rest]
        else
          params << [:rest, name]
        end
      elsif i < p
        params << [required_status, name]
      elsif code.block_index == i
        params << [:block, name]
      end
    end

    params
  end


  def to_proc
    self
  end

  alias_method :[], :call
  alias_method :yield, :call

  def clone
    copy = self.class.__allocate__
    Rubinius.invoke_primitive :object_copy_object, copy, self
    Rubinius.invoke_primitive :object_copy_singleton_class, copy, self

    Rubinius.privately do
      copy.initialize_copy self
    end

    copy.freeze if frozen?
    copy
  end

  Truffle.omit("We handle the dup in a parent class.") do
    def dup
      copy = self.class.__allocate__
      Rubinius.invoke_primitive :object_copy_object, copy, self

      Rubinius.privately do
        copy.initialize_copy self
      end
      copy
    end
  end

  def self.from_method(meth)
    if meth.kind_of? Method
      return __from_method__(meth)
    else
      raise ArgumentError, "tried to create a Proc object without a Method"
    end
  end
end
