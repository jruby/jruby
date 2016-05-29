# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle
  module CExt
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

    def rb_eRuntimeError
      raise 'not implemented'
    end

    def NIL_P(value)
      nil.equal?(value)
    end

    def FIXNUM_P(value)
      value.is_a?(Fixnum)
    end

    def rb_float_new(value)
      value.to_f
    end

    def RSTRING_PTR(string)
      string
    end

    def rb_intern
      raise 'not implemented'
    end

    def rb_str_new_cstr(java_string)
      String.new(java_string)
    end

    def rb_intern_str(string)
      string.intern
    end

    def ID2SYM(id)
      id
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

    def rb_scan_args
      raise 'not implemented'
    end

    def rb_raise(object, name)
      raise 'not implemented'
    end

    def rb_define_class(name, superclass)
      if Object.const_defined?(name)
        klass = Object.const_get(name)
        if superclass != klass.superclass
          raise TypeError, "superclass mismatch for class #{name}"
        end
        klass
      else
        Object.const_set(name, Class.new(superclass))
      end
    end

    def rb_define_module(name)
      Object.const_set(name, Module.new)
    end

    def rb_define_module_under(mod, name)
      mod.const_set(name, Module.new)
    end

    def rb_define_method(mod, name, function, args)
      mod.send(:define_method, name) do |*args|
        function.call(self, *args)
      end
    end

    def rb_define_private_method(mod, name, function, args)
      rb_define_method mod, name, function, args
      mod.send :private, name
    end

    def rb_define_module_function(mod, name, function, args)
      rb_define_method mod, name, function, args
      mod.send :module_function, name
    end

  end
end
