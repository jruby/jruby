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

# Copyright (c) 2011, Evan Phoenix
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
# * Neither the name of the Evan Phoenix nor the names of its contributors
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

# The Type module provides facilities for accessing various "type" related
# data about an object, as well as providing type coercion methods. These
# facilities are independent of the object and thus are more robust in the
# face of ad hoc monkeypatching.
module Rubinius
  module Type
    ##
    # Returns an object of given class. If given object already is one, it is
    # returned. Otherwise tries obj.meth and returns the result if it is of the
    # right kind. TypeErrors are raised if the conversion method fails or the
    # conversion result is wrong.
    #
    # Uses Rubinius::Type.object_kind_of to bypass type check overrides.
    #
    # Equivalent to MRI's rb_convert_type().

    def self.coerce_to(obj, cls, meth)
      return obj if object_kind_of?(obj, cls)
      begin
        ret = obj.__send__(meth)
      rescue Exception => orig
        raise TypeError,
              "Coercion error: #{obj.inspect}.#{meth} => #{cls} failed",
              orig
      end
      return ret if object_kind_of?(ret, cls)
      msg = "Coercion error: obj.#{meth} did NOT return a #{cls} (was #{object_class(ret)})"
      raise TypeError, msg
    end

    ##
    # Same as coerce_to but returns nil if conversion fails.
    # Corresponds to MRI's rb_check_convert_type()
    #
    def self.try_convert(obj, cls, meth)
      return obj if object_kind_of?(obj, cls)
      return nil unless obj.respond_to?(meth)

      begin
        ret = obj.__send__(meth)
      rescue Exception
        return nil
      end

      return ret if ret.nil? || object_kind_of?(ret, cls)

      msg = "Coercion error: obj.#{meth} did NOT return a #{cls} (was #{object_class(ret)})"
      raise TypeError, msg
    end

    def self.coerce_to_symbol(obj)
      if object_kind_of?(obj, Fixnum)
        raise ArgumentError, "Fixnums (#{obj}) cannot be used as symbols"
      end
      obj = obj.to_str if obj.respond_to?(:to_str)

      coerce_to(obj, Symbol, :to_sym)
    end

    def self.coerce_to_comparison(a, b)
      unless cmp = (a <=> b)
        raise ArgumentError, "comparison of #{a.inspect} with #{b.inspect} failed"
      end
      cmp
    end

    # Maps to rb_num2long in MRI
    def self.num2long(obj)
      if obj == nil
        raise TypeError, "no implicit conversion from nil to integer"
      else
        Integer(obj)
      end
    end

    def self.each_ancestor(mod)
      unless object_kind_of?(mod, Class) and singleton_class_object(mod)
        yield mod
      end

      sup = mod.direct_superclass()
      while sup
        if object_kind_of?(sup, IncludedModule)
          yield sup.module
        elsif object_kind_of?(sup, Class)
          yield sup unless singleton_class_object(sup)
        else
          yield sup
        end
        sup = sup.direct_superclass()
      end
    end

    def self.ivar_validate(name)
      # adapted from rb_to_id
      case name
        when String
          return name.to_sym if name[0] == ?@
        when Symbol
          return name if name.is_ivar?
        when Fixnum
          raise ArgumentError, "#{name.inspect} is not a symbol"
        else
          name = Rubinius::Type.coerce_to(name, String, :to_str)
          return name.to_sym if name[0] == ?@
      end

      raise NameError, "`#{name}' is not allowed as an instance variable name"
    end

    def self.object_kind_of?(obj, cls)
      obj.class <= cls
    end

    # Performs a direct kind_of? check on the object bypassing any method
    # overrides.
    def self.object_kind_of?(obj, cls)
      Rubinius.primitive :vm_object_kind_of
      raise PrimitiveFailure, "Rubinius::Type.object_kind_of? primitive failed"
    end

    def self.object_class(obj)
      Rubinius.primitive :vm_object_class
      raise PrimitiveFailure, "Rubinius::Type.object_class primitive failed"
    end

    def self.object_singleton_class(obj)
      Rubinius.primitive :vm_object_singleton_class
      raise TypeError, "no singleton class available for a #{Type.object_class(obj)}"
    end

    def self.singleton_class_object(mod)
      Rubinius.primitive :vm_singleton_class_object
      raise PrimitiveFailure, "Rubinius::Type.singleton_class_object primitive failed"
    end

    def self.object_instance_of?(obj, cls)
      object_class(obj) == cls
    end

    def self.object_respond_to?(obj, name, include_private = false)
      Rubinius.invoke_primitive :vm_object_respond_to, obj, name, include_private
    end

    def self.object_equal(a, b)
      Rubinius.primitive :vm_object_equal
      raise PrimitiveFailure, "Rubinius::Type.object_equal primitive failed"
    end

    def self.module_name(mod)
      Rubinius.primitive :vm_get_module_name
      raise PrimitiveFailure, "Rubinius::Type.module_name primitive failed"
    end

    def self.module_inspect(mod)
      sc = singleton_class_object mod

      if sc
        case sc
        when Class, Module
          name = "#<Class:#{module_inspect(sc)}>"
        else
          cls = object_class sc
          name = "#<Class:#<#{module_name(cls)}:0x#{sc.object_id.to_s(16)}>>"
        end
      else
        name = module_name mod
        if !name or name == ""
          name = "#<#{object_class(mod)}:0x#{mod.object_id.to_s(16)}>"
        end
      end

      name
    end

    def self.set_module_name(mod, name, under)
      Rubinius.primitive :vm_set_module_name
      raise PrimitiveFailure, "Rubinius::Type.set_module_name primitive failed"
    end

    def self.coerce_string_to_float(string, strict)
      value = Rubinius.invoke_primitive :string_to_f, string, strict
      raise ArgumentError, "invalid string for Float" if value.nil?
      value
    end

    def self.coerce_to_array(obj)
      return [obj] unless obj

      return Rubinius.privately { obj.to_a } if object_respond_to?(obj, :to_a, true)
      return obj.to_ary if obj.respond_to?(:to_ary)

      # On 1.9, #to_a is not defined on all objects, so wrap the object in a
      # literal array.
      return [obj]
    end

    def self.coerce_to_float(obj, strict=true, must_be_numeric=true)
      if !must_be_numeric && object_kind_of?(obj, String)
        return coerce_string_to_float(obj, strict)
      end

      case obj
      when Float
        obj
      when Numeric
        coerce_to obj, Float, :to_f
      when nil, true, false
        raise TypeError, "can't convert #{obj.inspect} into Float"
      else
        raise TypeError, "can't convert #{obj.class} into Float"
      end
    end

    def self.coerce_object_to_float(obj)
      case obj
      when Float
        obj
      when nil
        raise TypeError, "can't convert nil into Float"
      when Complex
        if obj.respond_to?(:imag) && obj.imag.equal?(0)
          coerce_to obj, Float, :to_f
        else
          raise RangeError, "can't convert #{obj} into Float"
        end
      else
        coerce_to obj, Float, :to_f
      end
    end

    def self.object_encoding(obj)
      Rubinius.primitive :encoding_get_object_encoding
      raise PrimitiveFailure, "Rubinius::Type.object_encoding primitive failed"
    end
  end
end
