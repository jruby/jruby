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
