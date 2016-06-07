# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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

##
# Some terminology notes:
#
# [Encloser] The Class or Module inside which this one is defined or, in the
#            event we are at top-level, Object.
#
# [Direct superclass] Whatever is next in the chain of superclass invocations.
#                     This may be either an included Module, a Class or nil.
#
# [Superclass] The real semantic superclass and thus only applies to Class
#              objects.

class Module

  attr_reader :constant_table
  attr_writer :method_table

  private :included

  private :attr

  def visibility_for_aliased_method(new_name, current_name_visibility)
    special_methods = [
      :initialize,
      :initialize_copy,
      :initialize_clone,
      :initialize_dup,
      :respond_to_missing?
    ]

    if special_methods.include?(new_name)
      :private
    else
      current_name_visibility
    end
  end
  private :visibility_for_aliased_method

  def verify_class_variable_name(name)
    if name.kind_of? Symbol
      return name if name.is_cvar?
      raise NameError, "#{name} is not an allowed class variable name"
    end

    name = StringValue(name).to_sym
    unless name.is_cvar?
      raise NameError, "#{name} is not an allowed class variable name"
    end
    name
  end
  private :verify_class_variable_name

  def __class_variables__
    Truffle.primitive :module_class_variables

    raise PrimitiveFailure, "Module#__class_variables__ primitive failed"
  end

  def lookup_method(sym, check_object_too=true, trim_im=true)
    mod = self

    while mod
      if mod == mod.origin && entry = mod.method_table.lookup(sym.to_sym)
        mod = mod.module if trim_im and mod.kind_of? Rubinius::IncludedModule
        return [mod, entry]
      end

      mod = mod.direct_superclass
    end

    # Optionally also search Object (and everything included in Object);
    # this lets a module alias methods on Object or Kernel.
    if check_object_too and instance_of?(Module)
      return Object.lookup_method(sym, true, trim_im)
    end
  end

  def filter_methods(filter, all)
    ary = []
    if all and self.direct_superclass
      table = Rubinius::LookupTable.new
      mod = self

      while mod
        mod.method_table.each do |name, obj, vis|
          unless table.key?(name)
            table[name] = true
            ary << name if vis == filter
          end
        end

        mod = mod.direct_superclass
      end
    else
      method_table.each do |name, obj, vis|
        ary << name if vis == filter
      end
    end

    ary
  end

  private :filter_methods

  private :define_method

  def thunk_method(name, value)
    thunk = Rubinius::Thunk.new(value)
    name = Rubinius::Type.coerce_to_symbol name
    Rubinius.add_method name, thunk, self, :public

    name
  end

  def include?(mod)
    if !mod.kind_of?(Module) or mod.kind_of?(Class)
      raise TypeError, "wrong argument type #{mod.class} (expected Module)"
    end

    return false if self.equal?(mod)

    Rubinius::Type.each_ancestor(self) { |m| return true if mod.equal?(m) }

    false
  end

  def set_visibility(meth, vis, where=nil)
    name = Rubinius::Type.coerce_to_symbol(meth)
    vis = vis.to_sym

    if entry = @method_table.lookup(name)
      entry.visibility = vis
    elsif lookup_method(name)
      @method_table.store name, nil, vis
    else
      raise NameError.new("Unknown #{where}method '#{name}' to make #{vis.to_s} (#{self})", name)
    end

    Rubinius::VM.reset_method_cache self, name

    return name
  end

  def set_class_visibility(meth, vis)
    Rubinius::Type.object_singleton_class(self).set_visibility meth, vis, "class "
  end
  private :set_class_visibility

  alias_method :class_exec, :module_exec

  private :remove_const

  def extended(name)
  end

  private :extended

  def method_added(name)
  end

  private :method_added

  def method_removed(name)
  end

  private :method_removed

  def method_undefined(name)
  end

  private :method_undefined

  private :initialize_copy

  def add_ivars(code)
    case code
    when Rubinius::CompiledCode
      new_ivars = code.literals.select { |l| l.kind_of?(Symbol) and l.is_ivar? }
      return if new_ivars.empty?

      if @seen_ivars
        new_ivars.each do |x|
          unless @seen_ivars.include?(x)
            @seen_ivars << x
          end
        end
      else
        @seen_ivars = new_ivars
      end
    when Rubinius::AccessVariable
      if @seen_ivars
        unless @seen_ivars.include?(code.name)
          @seen_ivars << code.name
        end
      else
        @seen_ivars = [code.name]
      end
    else
      raise "Unknown type of method to learn ivars - #{code.class}"
    end
  end

  def dynamic_method(name, file="(dynamic)", line=1)
    g = Rubinius::ToolSets::Runtime::Generator.new
    g.name = name.to_sym
    g.file = file.to_sym
    g.set_line Integer(line)

    yield g

    g.close

    g.use_detected
    g.encode

    code = g.package Rubinius::CompiledCode

    code.scope =
      Rubinius::ConstantScope.new(self, Rubinius::ConstantScope.new(Object))

    Rubinius.add_method name, code, self, :public

    return code
  end

  def freeze
    @method_table.freeze
    @constant_table.freeze
    super
  end

  # Not sure what this does - but we seem better off not doing it
  def attr_reader_specific(a, b)
  end

  def prepended(mod); end
  private :prepended

  def prepend(*modules)
    modules.reverse_each do |mod|
      if !mod.kind_of?(Module) or mod.kind_of?(Class)
        raise TypeError, "wrong argument type #{mod.class} (expected Module)"
      end

      Truffle.privately do
        mod.prepend_features self
      end

      Truffle.privately do
        mod.prepended self
      end
    end
    self
  end

  def self.constants(inherited = undefined)
    if undefined.equal?(inherited)
      Object.constants
    else
      super(inherited)
    end
  end
end
