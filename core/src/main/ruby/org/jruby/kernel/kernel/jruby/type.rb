module JRuby
  module Type
    # Helper method to coerce a value into a specific class.
    # Raises a TypeError if the coercion fails or the returned value
    # is not of the right class.
    # (from Rubinius)
    def self.coerce_to(obj, cls, meth)
      return obj if obj.kind_of?(cls)

      begin
        ret = obj.__send__(meth)
      rescue Exception => e
        raise TypeError, "Coercion error: #{obj.inspect}.#{meth} => #{cls} failed:\n" \
          "(#{e.message})"
      end
      raise TypeError, "Coercion error: obj.#{meth} did NOT return a #{cls} (was #{ret.class})" unless ret.kind_of? cls
      ret
    end

    def self.coerce_to_int(obj)
      coerce_to(obj, Integer, :to_int)
    end

    def self.coerce_to_ary(obj)
      coerce_to(obj, Array, :to_ary)
    end

    def self.coerce_to_str(obj)
      coerce_to(obj, String, :to_str)
    end

    def self.is_array?(obj)
      coerce_to(obj, Array, :to_ary) if object_respond_to?(obj, :to_ary)
    end

    def self.convert_to_str(obj)
      unless obj.respond_to? :to_str
        raise TypeError, "cannot convert #{obj.class} into String"
      end
      obj.to_str
    end

    def self.object_respond_to?(obj, method)
      obj.respond_to?(method) # NOTE: should handle BasicObject? (rescue nil)
    end
  end
end