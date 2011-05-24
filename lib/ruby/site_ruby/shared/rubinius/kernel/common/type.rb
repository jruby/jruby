##
# Namespace for coercion functions between various ruby objects.

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
  end
end
