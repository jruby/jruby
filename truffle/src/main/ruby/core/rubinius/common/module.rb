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

  def self.nesting
    scope = Rubinius::ConstantScope.of_sender
    nesting = []
    while scope and scope.module != Object
      nesting << scope.module
      scope = scope.parent
    end
    nesting
  end

  def initialize(&block)
    # The method and constants tables are created and setup
    # by Class and Module allocate.
    module_eval(&block) if block
  end

  def const_get(name, inherit=true)
    Rubinius::Type.const_get self, name, inherit
  end

  def const_defined?(name, inherit=true)
    c = Rubinius::Type.const_get self, name, inherit, false
    return c.equal?(undefined) ? false : true
  end

  def attr(*attributes)
    vis = Rubinius::VariableScope.of_sender.method_visibility

    if attributes.size == 2 && (attributes.last.is_a?(TrueClass) || attributes.last.is_a?(FalseClass))
      name = attributes.first
      Rubinius.add_reader name, self, vis
      Rubinius.add_writer name, self, vis if attributes.last
    else
      attributes.each do |name|
        if name.is_a?(Symbol) || (name.respond_to?(:to_str) && name.to_str.is_a?(String) && !name.to_str.empty?)
          Rubinius.add_reader(name, self, vis)
        else
          raise TypeError, "#{name.inspect} is not a symbol"
        end
      end
    end

    return nil
  end

  private :attr

  # Install a new Autoload object into the constants table
  # See kernel/common/autoload.rb
  def autoload(name, path)
    path = Rubinius::Type.coerce_to_path(path)

    raise ArgumentError, "empty file name" if path.empty?

    return if Rubinius::CodeLoader.feature_provided?(path)

    name = Rubinius::Type.coerce_to_constant_name name

    Rubinius.check_frozen

    if entry = @constant_table.lookup(name)
      if entry.constant.kind_of? Autoload
        # If there is already an Autoload here, just change the path to
        # autoload!
        entry.constant.set_path(path)
      else
        # Trying to register an autoload for a constant that already exists,
        # ignore the request entirely.
      end

      return
    end

    autoload_contant = Autoload.new(name, self, path)
    constant_table.store(name, autoload_contant, :public)
    if self == Kernel
      Object.singleton_class.constant_table.store(name, autoload_contant, :public)
    end

    Rubinius.inc_global_serial
    return nil
  end

  def constants(all=undefined)
    tbl = Rubinius::LookupTable.new

    @constant_table.each do |name, constant, visibility|
      tbl[name] = true unless visibility == :private
    end

    if all
      current = self.direct_superclass

      while current and current != Object
        current.constant_table.each do |name, constant, visibility|
          tbl[name] = true unless visibility == :private
        end

        current = current.direct_superclass
      end
    end

    # special case: Module.constants returns Object's constants
    if self.equal?(Module) && undefined.equal?(all)
      Object.constant_table.each do |name, constant, visibility|
        tbl[name] = true unless visibility == :private
      end
    end

    tbl.keys
  end

  def private_constant(*names)
    names = names.map(&:to_sym)
    unknown_constants = names - @constant_table.keys
    if unknown_constants.size > 0
      raise NameError, "#{unknown_constants.size > 1 ? 'Constants' : 'Constant'} #{unknown_constants.map{|e| "#{name}::#{e}"}.join(', ')} undefined"
    end
    names.each do |name|
      entry = @constant_table.lookup(name)
      entry.visibility = :private
    end
  end

  def public_constant(*names)
    names = names.map(&:to_sym)
    unknown_constants = names - @constant_table.keys
    if unknown_constants.size > 0
      raise NameError, "#{unknown_constants.size > 1 ? 'Constants' : 'Constant'} #{unknown_constants.map{|e| "#{name}::#{e}"}.join(', ')} undefined"
    end
    names.each do |name|
      entry = @constant_table.lookup(name)
      entry.visibility = :public
    end
  end

  ##
  # Returns an UnboundMethod corresponding to the given name. The name will be
  # searched for in this Module as well as any included Modules or
  # superclasses. The UnboundMethod is populated with the method name and the
  # Module that the method was located in.
  #
  # Raises a TypeError if the given name.to_sym fails and a NameError if the
  # name cannot be located.

  def instance_method(name)
    name = Rubinius::Type.coerce_to_symbol name

    mod = self
    while mod
      if mod == mod.origin && entry = mod.method_table.lookup(name)
        break if entry.visibility == :undef

        if meth = entry.method
          if meth.kind_of? Rubinius::Alias
            meth = meth.original_exec
          end

          mod = mod.module if mod.class == Rubinius::IncludedModule

          return UnboundMethod.new(mod, meth, self, name)
        end
      end

      mod = mod.direct_superclass
    end

    raise NameError.new("undefined method `#{name}' for #{self}", name)
  end

  def public_instance_method(name)
    name = Rubinius::Type.coerce_to_symbol name

    mod = self
    while mod
      if mod == mod.origin && entry = mod.method_table.lookup(name)
        vis = entry.visibility
        break if vis == :undef

        if meth = entry.method
          if meth.kind_of? Rubinius::Alias
            meth = meth.original_exec
          end

          mod = mod.module if mod.class == Rubinius::IncludedModule

          if vis == :public
            return UnboundMethod.new(mod, meth, self, name)
          else
            raise NameError.new("method `#{name}' for module `#{self}' is #{vis}", name)
          end
        end
      end

      mod = mod.direct_superclass
    end

    raise NameError.new("undefined method `#{name}' for #{self}", name)
  end

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

  def class_variable_set(name, val)
    Rubinius.primitive :module_cvar_set

    class_variable_set verify_class_variable_name(name), val
  end

  def class_variable_get(name)
    Rubinius.primitive :module_cvar_get

    class_variable_get verify_class_variable_name(name)
  end

  def class_variable_defined?(name)
    Rubinius.primitive :module_cvar_defined

    class_variable_defined? verify_class_variable_name(name)
  end

  def remove_class_variable(name)
    Rubinius.primitive :module_cvar_remove

    remove_class_variable verify_class_variable_name(name)
  end

  def __class_variables__
    Rubinius.primitive :module_class_variables

    raise PrimitiveFailure, "Module#__class_variables__ primitive failed"
  end
  alias_method :class_variables, :__class_variables__

  def name
    Rubinius::Type.module_name self
  end

  def to_s
    Rubinius::Type.module_inspect self
  end

  def inspect
    to_s
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

  def ancestors
    out = []
    Rubinius::Type.each_ancestor(self) { |mod| out << mod }
    return out
  end

  def undef_method(*names)
    return self if names.empty?
    names.map!{ |name| Rubinius::Type.coerce_to_symbol name }
    Rubinius.check_frozen

    names.each do |name|
      # Will raise a NameError if the method doesn't exist.
      instance_method name
      undef_method! name
    end

    self
  end

  # Like undef_method, but doesn't even check that the method exists. Used
  # mainly to implement rb_undef_method.
  def undef_method!(name)
    Rubinius.check_frozen
    name = Rubinius::Type.coerce_to_symbol(name)
    @method_table.store name, nil, :undef
    Rubinius::VM.reset_method_cache self, name
    if obj = Rubinius::Type.singleton_class_object(self)
      Rubinius.privately do
        obj.singleton_method_undefined(name)
      end
    else
      method_undefined(name)
    end

    name
  end

  def remove_method(*names)
    names.each do |name|
      name = Rubinius::Type.coerce_to_symbol(name)
      Rubinius.check_frozen

      unless @method_table.lookup(name)
        raise NameError.new("method `#{name}' not defined in #{self.name}", name)
      end
      @method_table.delete name

      Rubinius::VM.reset_method_cache self, name

      if obj = Rubinius::Type.singleton_class_object(self)
        Rubinius.privately do
          obj.singleton_method_removed(name)
        end
      else
        method_removed(name)
      end
    end

    self
  end

  def public_method_defined?(sym)
    sym = Rubinius::Type.coerce_to_reflection_name(sym)
    mod, meth = lookup_method(sym, false)
    meth ? meth.public? : false
  end

  def private_method_defined?(sym)
    sym = Rubinius::Type.coerce_to_reflection_name(sym)
    mod, meth = lookup_method(sym, false)
    meth ? meth.private? : false
  end

  def protected_method_defined?(sym)
    sym = Rubinius::Type.coerce_to_reflection_name(sym)
    mod, meth = lookup_method(sym, false)
    meth ? meth.protected? : false
  end

  def method_defined?(sym)
    sym = Rubinius::Type.coerce_to_symbol(sym)
    mod, meth = lookup_method(sym, false)
    meth ? meth.public? || meth.protected? : false
  end

  def instance_methods(all=true)
    ary = []
    if all
      table = Rubinius::LookupTable.new

      mod = self

      while mod
        mod.method_table.each do |name, obj, vis|
          unless table.key?(name)
            table[name] = true
            ary << name if vis == :public or vis == :protected
          end
        end

        mod = mod.direct_superclass
      end
    else
      method_table.each do |name, obj, vis|
        ary << name if vis == :public or vis == :protected
      end
    end

    ary
  end

  def public_instance_methods(all=true)
    filter_methods(:public, all)
  end

  def private_instance_methods(all=true)
    filter_methods(:private, all)
  end

  def protected_instance_methods(all=true)
    filter_methods(:protected, all)
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

  def define_method(name, meth = undefined, &prc)
    if undefined.equal?(meth) and !block_given?
      raise ArgumentError, "tried to create Proc object without a block"
    end

    meth = prc if undefined.equal?(meth)

    name = Rubinius::Type.coerce_to_symbol name

    case meth
    when Proc
      if meth.ruby_method
        code = Rubinius::DelegatedMethod.new(name, :call, meth, false)
      else
        be = meth.block.dup
        be.change_name name
        code = Rubinius::BlockEnvironment::AsMethod.new(be)
        meth = meth.dup
        meth.lambda_style!
      end
    when Method
      Rubinius::Type.bindable_method? meth.defined_in, self.class

      exec = meth.executable
      # We see through delegated methods because code creates these crazy calls
      # to define_method over and over again and if we don't check, we create
      # a huge delegated method chain. So instead, just see through them at one
      # level always.
      if exec.kind_of? Rubinius::DelegatedMethod
        code = exec
      else
        code = Rubinius::DelegatedMethod.new(name, :call_on_instance, meth.unbind, true)
      end
    when UnboundMethod
      Rubinius::Type.bindable_method? meth.defined_in, self.class

      exec = meth.executable
      # Same reasoning as above.
      if exec.kind_of? Rubinius::DelegatedMethod
        code = exec
      else
        code = Rubinius::DelegatedMethod.new(name, :call_on_instance, meth, true)
      end
    else
      raise TypeError, "wrong argument type #{meth.class} (expected Proc/Method)"
    end

    Rubinius.add_method name, code, self, :public

    name
  end

  private :define_method

  def thunk_method(name, value)
    thunk = Rubinius::Thunk.new(value)
    name = Rubinius::Type.coerce_to_symbol name
    Rubinius.add_method name, thunk, self, :public

    name
  end

  def extend_object(obj)
    include_into Rubinius::Type.object_singleton_class(obj)
  end

  def include?(mod)
    if !mod.kind_of?(Module) or mod.kind_of?(Class)
      raise TypeError, "wrong argument type #{mod.class} (expected Module)"
    end

    return false if self.equal?(mod)

    Rubinius::Type.each_ancestor(self) { |m| return true if mod.equal?(m) }

    false
  end

  def included_modules
    out = []
    sup = direct_superclass

    while sup
      if sup.class == Rubinius::IncludedModule
        out << sup.module
      end

      sup = sup.direct_superclass
    end

    out
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

  def protected(*args)
    if args.empty?
      vs = Rubinius::VariableScope.of_sender
      vs.method_visibility = :protected
    else
      args.each { |meth| set_visibility(meth, :protected) }
    end

    self
  end

  def private_class_method(*args)
    args.each do |meth|
      set_class_visibility(meth, :private)
    end
    self
  end

  def public_class_method(*args)
    args.each do |meth|
      set_class_visibility(meth, :public)
    end
    self
  end

  def module_exec(*args, &prc)
    raise LocalJumpError, "Missing block" unless block_given?

    env = prc.block
    constant_scope = env.repoint_scope self
    return env.call_under(self, constant_scope, true, *args)
  end
  alias_method :class_exec, :module_exec

  def const_set(name, value)
    name = Rubinius::Type.coerce_to_constant_name name
    Rubinius.check_frozen

    if Rubinius::Type.object_kind_of? value, Module
      Rubinius::Type.set_module_name value, name, self

      if Object.equal? self
        value.constant_table.each do |n, c, _|
          if Rubinius::Type.object_kind_of? c, Module and !c.name
            Rubinius::Type.set_module_name c, n, value
          end
        end
      end
    end

    @constant_table.store(name, value, :public)
    Rubinius.inc_global_serial

    return value
  end

  ##
  # Return the named constant enclosed in this Module.
  #
  # Included Modules and, for Class objects, superclasses are also searched.
  # Modules will in addition look in Object. The name is attempted to convert
  # using #to_str. If the constant is not found, #const_missing is called
  # with the name.

  def const_missing(name)
    unless self == Object
      mod_name = "#{Rubinius::Type.module_name self}::"
    end
    case Rubinius.constant_missing_reason
    when :private
      msg = "Private constant: #{mod_name}#{name}"
    else
      msg = "Missing or uninitialized constant: #{mod_name}#{name}"
    end

    raise NameError.new(msg, name)
  end

  def <(other)
    unless other.kind_of? Module
      raise TypeError, "compared with non class/module"
    end

    # We're not an ancestor of ourself
    return false if self.equal? other

    Rubinius::Type.each_ancestor(self) { |mod| return true if mod.equal?(other) }
    Rubinius::Type.each_ancestor(other) { |mod| return false if mod.equal?(self) }

    nil
  end

  def <=(other)
    equal?(other) || (self < other)
  end

  def >(other)
    unless other.kind_of? Module
      raise TypeError, "compared with non class/module"
    end
    other < self
  end

  def >=(other)
    equal?(other) || (self > other)
  end

  def <=>(other)
    return 0 if self.equal? other
    return nil unless other.kind_of? Module

    lt = self < other
    return nil if lt.nil?
    lt ? -1 : 1
  end

  def ===(inst)
    Rubinius::Type.object_kind_of?(inst, self)
  end

  # Is an autoload trigger defined for the given path?
  def autoload?(name)
    name = name.to_sym
    entry = constant_table.lookup(name)
    return unless entry
    trigger = entry.constant
    return unless trigger && trigger.kind_of?(Autoload)
    trigger.path
  end

  def remove_const(name)
    unless name.kind_of? Symbol
      name = StringValue name

      unless Rubinius::CType.isupper name.getbyte(0)
        raise NameError, "constant names must begin with a capital letter: #{name}"
      end
    end

    sym = name.to_sym
    unless constant_table.has_name?(sym)
      mod_name = Rubinius::Type.module_name self
      raise NameError, "Missing or uninitialized constant: #{mod_name}::#{name}"
    end

    val = constant_table.delete(sym)
    Rubinius.inc_global_serial

    # Silly API compact. Shield Autoload instances
    return nil if Rubinius::Type.object_kind_of?(val, Autoload)
    val
  end

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

  def initialize_copy(other)
    # If the method table is already different, we already
    # initialized this module.
    unless @method_table == other.method_table
      raise TypeError, "already initialized module"
    end

    @method_table = other.method_table.dup

    sc_s = Rubinius::Type.object_singleton_class self
    sc_o = Rubinius::Type.object_singleton_class other

    sc_s.method_table = sc_o.method_table.dup
    sc_s.superclass = sc_o.direct_superclass

    @constant_table = Rubinius::ConstantTable.new

    other.constant_table.each do |name, val|
      if val.kind_of? Autoload
        val = Autoload.new(val.name, self, val.path)
      end

      @constant_table.store(name, val, :public)
    end

    if other == other.origin
      @origin = self
    else
      @origin = other.origin.dup
    end

    self
  end

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

  def attr_reader(name)
    meth = Rubinius::AccessVariable.get_ivar name
    @method_table.store name, meth, :public
    Rubinius::VM.reset_method_cache self, name
    ivar_name = "@#{name}".to_sym
    if @seen_ivars
      @seen_ivars.each do |ivar|
        return nil if ivar == ivar_name
      end
      @seen_ivars[@seen_ivars.size] = ivar_name
    else
      @seen_ivars = [ivar_name]
    end
    nil
  end

  def attr_writer(name)
    meth = Rubinius::AccessVariable.set_ivar name
    writer_name = "#{name}=".to_sym
    @method_table.store writer_name, meth, :public
    Rubinius::VM.reset_method_cache self, writer_name
    ivar_name = "@#{name}".to_sym
    if @seen_ivars
      @seen_ivars.each do |ivar|
        return nil if ivar == ivar_name
      end
      @seen_ivars[@seen_ivars.size] = ivar_name
    else
      @seen_ivars = [ivar_name]
    end
    nil
  end

  # Not sure what this does - but we seem better off not doing it
  def attr_reader_specific(a, b)
  end
end
