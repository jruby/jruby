# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle::CExt
  module_function

  def supported?
    Interop.mime_type_supported?('application/x-sulong-library')
  end

  def Qfalse
    false
  end

  def Qtrue
    true
  end

  def Qnil
    nil
  end

  def rb_cObject
    Object
  end

  def rb_cArray
    Array
  end

  def rb_cHash
    Hash
  end

  def rb_cProc
    Proc
  end

  def rb_mKernel
    Kernel
  end

  def rb_eRuntimeError
    raise 'not implemented'
  end

  def NIL_P(value)
    nil.equal?(value)
  end

  def FIXNUM_P(value)
    value.is_a?(Fixnum)
  end

  def RTEST(value)
    !nil.equal?(value) && !false.equal?(value)
  end

  def rb_float_new(value)
    value.to_f
  end

  def RSTRING_PTR(string)
    Truffle::Interop.to_java_string(string)
  end

  def rb_intern(str)
    str.intern
  end

  def rb_str_new_cstr(java_string)
    String.new(java_string)
  end

  def rb_intern_str(string)
    string.intern
  end

  def rb_str_cat(string, to_concat, length)
    raise 'not implemented'
  end

  def RARRAY_PTR(array)
    array
  end

  def rb_Array(value)
    Array(value)
  end

  def rb_ary_new
    []
  end

  def rb_ary_new_capa(capacity)
    []
  end

  def rb_hash_new
    {}
  end

  def rb_proc_new(function, value)
    proc { |*args|
      Truffle::Interop.execute(function, *args)
    }
  end

  def rb_scan_args
    raise 'not implemented'
  end

  def rb_yield(value)
    block = get_block
    block.call(value)
  end

  def rb_raise(object, name)
    raise 'not implemented'
  end

  def rb_define_class_under(mod, name, superclass)
    if mod.const_defined?(name)
      klass = mod.const_get(name)
      unless klass.class == Class
        raise TypeError, "#{mod}::#{name} is not a class"
      end
      if superclass != klass.superclass
        raise TypeError, "superclass mismatch for class #{name}"
      end
      klass
    else
      mod.const_set(name, Class.new(superclass))
    end
  end

  def rb_define_module_under(mod, name)
    if mod.const_defined?(name)
      val = mod.const_get(name)
      unless val.class == Module
        raise TypeError, "#{mod}::#{name} is not a module"
      end
      val
    else
      mod.const_set(name, Module.new)
    end
  end

  def rb_define_method(mod, name, function, argc)
    mod.send(:define_method, name) do |*args|
      # Using raw execute instead of #call here to avoid argument conversion
      Truffle::Interop.execute(function, self, *args)
    end
  end

  def rb_define_private_method(mod, name, function, argc)
    rb_define_method(mod, name, function, argc)
    mod.send :private, name
  end

  def rb_define_protected_method(mod, name, function, argc)
    rb_define_method(mod, name, function, argc)
    mod.send :protected, name
  end

  def rb_define_module_function(mod, name, function, argc)
    rb_define_method(mod, name, function, argc)
    mod.send :module_function, name
  end

  def rb_define_singleton_method(object, name, function, argc)
    rb_define_method(object.singleton_class, name, function, argc)
  end

  def rb_alias(mod, new_name, old_name)
    mod.send(:alias_method, new_name, old_name)
  end

  def rb_undef(mod, name)
    if mod.frozen? or mod.method_defined?(name)
      mod.send(:undef_method, name)
    end
  end

  def rb_funcall(object, name, argc, args)
    object.__send__(name, *args)
  end
end

Truffle::Interop.export(:ruby_cext, Truffle::CExt)
