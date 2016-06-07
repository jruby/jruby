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

##
# Namespace for coercion functions between various ruby objects.

module Rubinius
  module Type

    # Maps to rb_num2long in MRI
    def self.num2long(obj)
      if obj == nil
        raise TypeError, "no implicit conversion from nil to integer"
      else
        Integer(obj)
      end
    end

    # Performs a direct kind_of? check on the object bypassing any method
    # overrides.
    def self.object_kind_of?(obj, cls)
      Truffle.primitive :vm_object_kind_of
      raise PrimitiveFailure, "Rubinius::Type.object_kind_of? primitive failed"
    end

    def self.object_class(obj)
      Truffle.primitive :vm_object_class
      raise PrimitiveFailure, "Rubinius::Type.object_class primitive failed"
    end

    def self.object_singleton_class(obj)
      Truffle.primitive :vm_object_singleton_class
      raise TypeError, "no singleton class available for a #{Type.object_class(obj)}"
    end

    def self.singleton_class_object(mod)
      Truffle.primitive :vm_singleton_class_object
      raise PrimitiveFailure, "Rubinius::Type.singleton_class_object primitive failed"
    end

    def self.object_instance_of?(obj, cls)
      object_class(obj) == cls
    end

    def self.object_respond_to?(obj, name, include_private = false)
      Truffle.invoke_primitive :vm_object_respond_to, obj, name, include_private
    end

    def self.object_equal(a, b)
      Truffle.primitive :vm_object_equal
      raise PrimitiveFailure, "Rubinius::Type.object_equal primitive failed"
    end

    def self.module_name(mod)
      Truffle.primitive :vm_get_module_name
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
      Truffle.primitive :vm_set_module_name
      raise PrimitiveFailure, "Rubinius::Type.set_module_name primitive failed"
    end

    def self.coerce_string_to_float(string, strict)
      value = Truffle.invoke_primitive :string_to_f, string, strict
      raise ArgumentError, "invalid string for Float" if value.nil?
      value
    end

    def self.coerce_to_array(obj)
      return [obj] unless obj

      return Truffle.privately { obj.to_a } if object_respond_to?(obj, :to_a, true)
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
      Truffle.primitive :encoding_get_object_encoding
      raise PrimitiveFailure, "Rubinius::Type.object_encoding primitive failed"
    end

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
      execute_coerce_to(obj, cls, meth)
    end

    def self.execute_coerce_to(obj, cls, meth)
      begin
        ret = obj.__send__(meth)
      rescue Exception => orig
        coerce_to_failed obj, cls, meth, orig
      end

      return ret if object_kind_of?(ret, cls)

      coerce_to_type_error obj, ret, meth, cls
    end

    def self.coerce_to_failed(object, klass, method, exc=nil)
      if object_respond_to? object, :inspect
        raise TypeError,
            "Coercion error: #{object.inspect}.#{method} => #{klass} failed",
            exc
      else
        raise TypeError,
            "Coercion error: #{method} => #{klass} failed",
            exc
      end
    end

    def self.coerce_to_type_error(original, converted, method, klass)
      oc = object_class original
      cc = object_class converted
      msg = "failed to convert #{oc} to #{klass}: #{oc}\##{method} returned #{cc}"
      raise TypeError, msg
    end

    ##
    # Same as coerce_to but returns nil if conversion fails.
    # Corresponds to MRI's rb_check_convert_type()
    #
    def self.check_convert_type(obj, cls, meth)
      return obj if object_kind_of?(obj, cls)
      return nil unless object_respond_to?(obj, meth, true)
      execute_check_convert_type(obj, cls, meth)
    end

    def self.execute_check_convert_type(obj, cls, meth)
      begin
        ret = obj.__send__(meth)
      rescue Exception
        return nil
      end

      return ret if ret.nil? || object_kind_of?(ret, cls)

      msg = "Coercion error: obj.#{meth} did NOT return a #{cls} (was #{object_class(ret)})"
      raise TypeError, msg
    end

    ##
    # Uses the logic of [Array, Hash, String].try_convert.
    #
    def self.try_convert(obj, cls, meth)
      return obj if object_kind_of?(obj, cls)
      return nil unless obj.respond_to?(meth)
      execute_try_convert(obj, cls, meth)
    end

    def self.execute_try_convert(obj, cls, meth)
      ret = obj.__send__(meth)

      return ret if ret.nil? || object_kind_of?(ret, cls)

      msg = "Coercion error: obj.#{meth} did NOT return a #{cls} (was #{object_class(ret)})"
      raise TypeError, msg
    end

    def self.coerce_to_comparison(a, b)
      unless cmp = (a <=> b)
        raise ArgumentError, "comparison of #{a.inspect} with #{b.inspect} failed"
      end
      cmp
    end

    def self.coerce_to_constant_name(name)
      name = Rubinius::Type.coerce_to_symbol(name)

      unless name.is_constant?
        raise NameError, "wrong constant name #{name}"
      end

      name
    end

    def self.coerce_to_collection_index(index)
      return index if object_kind_of? index, Fixnum

      method = :to_int
      klass = Fixnum

      begin
        idx = index.__send__ method
      rescue Exception => exc
        coerce_to_failed index, klass, method, exc
      end
      return idx if object_kind_of? idx, klass

      if object_kind_of? index, Bignum
        raise RangeError, "Array index must be a Fixnum (passed Bignum)"
      else
        coerce_to_type_error index, idx, method, klass
      end
    end

    def self.coerce_to_collection_length(length)
      return length if object_kind_of? length, Fixnum

      method = :to_int
      klass = Fixnum

      begin
        size = length.__send__ method
      rescue Exception => exc
        coerce_to_failed length, klass, method, exc
      end
      return size if object_kind_of? size, klass

      if object_kind_of? size, Bignum
        raise ArgumentError, "Array size must be a Fixnum (passed Bignum)"
      else
        coerce_to_type_error length, size, :to_int, Fixnum
      end
    end

    def self.coerce_to_regexp(pattern, quote=false)
      case pattern
      when Regexp
        return pattern
      when String
        # nothing
      else
        pattern = StringValue(pattern)
      end

      pattern = Regexp.quote(pattern) if quote
      Regexp.new(pattern)
    end

    # Taint host if source is tainted.
    def self.infect(host, source)
      Truffle.primitive :object_infect
      raise PrimitiveFailure, "Object.infect primitive failed"
    end

    def self.check_null_safe(string)
      Truffle.invoke_primitive(:string_check_null_safe, string)
    end

    def self.const_lookup(mod, name, inherit, resolve)
      parts = name.split '::'

      if name.start_with? '::'
        mod = Object
        parts.shift
      end

      parts.each do |part|
        mod = const_get mod, part, inherit, resolve
      end

      mod
    end

    def self.const_get(mod, name, inherit=true, resolve=true)
      unless object_kind_of? name, Symbol
        name = StringValue(name)
        if name.index '::' and name.size > 2
          return const_lookup mod, name, inherit, resolve
        end
      end

      name = coerce_to_constant_name name
      current = mod
      constant = undefined

      while current and object_kind_of? current, Module
        if bucket = current.constant_table.lookup(name)
          constant = bucket.constant
          if resolve and object_kind_of? constant, Autoload
            constant = constant.call(current)
          end

          return constant
        end

        unless inherit
          return resolve ? mod.const_missing(name) : undefined
        end

        current = current.direct_superclass
      end

      if object_instance_of? mod, Module
        if bucket = Object.constant_table.lookup(name)
          constant = bucket.constant
          if resolve and object_kind_of? constant, Autoload
            constant = constant.call(current)
          end

          return constant
        end
      end

      resolve ? mod.const_missing(name) : undefined
    end

    def self.const_exists?(mod, name, inherit = true)
      name = coerce_to_constant_name name

      current = mod

      while current
        if bucket = current.constant_table.lookup(name)
          return !!bucket.constant
        end

        return false unless inherit

        current = current.direct_superclass
      end

      if instance_of?(Module)
        if bucket = Object.constant_table.lookup(name)
          return !!bucket.constant
        end
      end

      false
    end

    def self.include_modules_from(included_module, klass)
      insert_at = klass
      changed = false
      constants_changed = false

      mod = included_module

      while mod

        # Check for a cyclic include
        if mod == klass
          raise ArgumentError, "cyclic include detected"
        end

        if mod == mod.origin
          # Try and detect check_mod in klass's heirarchy, and where.
          #
          # I (emp) tried to use Module#< here, but we need to also know
          # where in the heirarchy the module is to change the insertion point.
          # Since Module#< doesn't report that, we're going to just search directly.
          #
          superclass_seen = false
          add = true

          k = klass.direct_superclass
          while k
            if k.kind_of? Rubinius::IncludedModule
              # Oh, we found it.
              if k == mod
                # ok, if we're still within the directly included modules
                # of klass, then put future things after mod, not at the
                # beginning.
                insert_at = k unless superclass_seen
                add = false
                break
              end
            else
              superclass_seen = true
            end

            k = k.direct_superclass
          end

          if add
            if mod.kind_of? Rubinius::IncludedModule
              original_mod = mod.module
            else
              original_mod = mod
            end

            im = Rubinius::IncludedModule.new(original_mod).attach_to insert_at
            insert_at = im

            changed = true
          end

          constants_changed ||= mod.constant_table.size > 0
        end

        mod = mod.direct_superclass
      end

      if changed
        included_module.method_table.each do |meth, obj, vis|
          Rubinius::VM.reset_method_cache klass, meth
        end
      end

      if constants_changed
        Rubinius.inc_global_serial
      end
    end

    def self.coerce_to_encoding(obj)
      case obj
      when Encoding
        return obj
      when String
        return Encoding.find obj
      else
        return Encoding.find StringValue(obj)
      end
    end

    def self.try_convert_to_encoding(obj)
      case obj
      when Encoding
        return obj
      when String
        str = obj
      else
        str = StringValue obj
      end

      key = str.upcase.to_sym

      pair = Encoding::EncodingMap[key]
      if pair
        index = pair.last
        return index && Encoding::EncodingList[index]
      end

      return undefined
    end

    def self.coerce_to_path(obj)
      if object_kind_of?(obj, String)
        obj
      else
        if object_respond_to? obj, :to_path
          obj = obj.to_path
        end

        StringValue(obj)
      end
    end

    def self.coerce_to_symbol(obj)
      return obj if object_kind_of? obj, Symbol

      obj = obj.to_str if obj.respond_to?(:to_str)
      coerce_to(obj, Symbol, :to_sym)
    end

    def self.coerce_to_reflection_name(obj)
      return obj if object_kind_of? obj, Symbol
      return obj if object_kind_of? obj, String
      coerce_to obj, String, :to_str
    end

    def self.ivar_validate(name)
      case name
      when Symbol
        # do nothing
      when String
        name = name.to_sym
      else
        name = Rubinius::Type.coerce_to(name, String, :to_str)
        name = name.to_sym
      end

      unless name.is_ivar?
        raise NameError, "`#{name}' is not allowed as an instance variable name"
      end

      name
    end

    def self.coerce_to_binding(obj)
      if obj.kind_of? Binding
        binding = obj
      elsif obj.kind_of? Proc
        raise TypeError, 'wrong argument type Proc (expected Binding)'
      elsif obj.respond_to? :to_binding
        binding = obj.to_binding
      else
        binding = obj
      end

      unless binding.kind_of? Binding
        raise ArgumentError, "unknown type of binding"
      end

      binding
    end

    # Equivalent of num_exact in MRI's time.c; used by Time methods.
    def self.coerce_to_exact_num(obj)
      if obj.kind_of?(Integer)
        obj
      elsif obj.kind_of?(String)
        raise TypeError, "can't convert #{obj} into an exact number"
      elsif obj.nil?
        raise TypeError, "can't convert nil into an exact number"
      else
        check_convert_type(obj, Rational, :to_r) || coerce_to(obj, Integer, :to_int)
      end
    end

    def self.coerce_to_utc_offset(offset)
      offset = String.try_convert(offset) || offset

      if offset.kind_of?(String)
        unless offset.encoding.ascii_compatible? && offset.match(/\A(\+|-)(\d\d):(\d\d)\z/)
          raise ArgumentError, '"+HH:MM" or "-HH:MM" expected for utc_offset'
        end

        offset = $2.to_i*60*60 + $3.to_i*60
        offset = -offset if $1.ord == 45
      else
        offset = Rubinius::Type.coerce_to_exact_num(offset)
      end

      if offset <= -86400 || offset >= 86400
        raise ArgumentError, "utc_offset out of range"
      end

      offset
    end

    def self.coerce_to_pid(obj)
      Rubinius::Type.coerce_to obj, Integer, :to_int
    end

    def self.coerce_to_bitwise_operand(obj)
      if object_kind_of? obj, Float
        raise TypeError, "can't convert Float into Integer for bitwise arithmetic"
      end
      coerce_to obj, Integer, :to_int
    end

    def self.object_initialize_dup(obj, copy)
      Truffle.privately do
        copy.initialize_dup obj
      end
    end

    def self.object_initialize_clone(obj, copy)
      Truffle.privately do
        copy.initialize_clone obj
      end
    end

    def self.object_respond_to_ary?(obj)
      object_respond_to?(obj, :to_ary, true)
    end

    def self.binary_string(string)
      string.force_encoding Encoding::BINARY
    end

    def self.external_string(string)
      string.force_encoding Encoding.default_external
    end

    def self.encode_string(string, enc)
      string.force_encoding enc
    end

    def self.ascii_compatible_encoding(string)
      compatible_encoding string, Encoding::US_ASCII
    end

    def self.compatible_encoding(a, b)
      enc = Encoding.compatible? a, b

      unless enc
        enc_a = object_encoding a
        enc_b = object_encoding b
        message = "undefined conversion "
        message << "for '#{a.inspect}' " if object_kind_of?(a, String)
        message << "from #{enc_a} to #{enc_b}"

        raise Encoding::CompatibilityError, message
      end

      enc
    end

    def self.encoding_order(a, b)
      index_a = Encoding::EncodingMap[a.name.upcase][1]
      index_b = Encoding::EncodingMap[b.name.upcase][1]
      index_a <=> index_b
    end

    def self.object_respond_to__dump?(obj)
      object_respond_to? obj, :_dump, true
    end

    def self.object_respond_to_marshal_dump?(obj)
      object_respond_to? obj, :marshal_dump, true
    end

    def self.object_respond_to_marshal_load?(obj)
      object_respond_to? obj, :marshal_load, true
    end

    def self.bindable_method?(source, destination)
      unless object_kind_of? source, Module or
             object_kind_of? source, destination
        if singleton_class_object source
          raise TypeError, "illegal attempt to rebind a singleton method to another object"
        end

        raise TypeError, "Must be bound to an object of kind #{source}"
      end
    end
  end
end
