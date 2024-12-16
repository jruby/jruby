class Data
  def self.define(*symbols, &)
    rest = Hash.new.compare_by_identity
    symbols.each do |symbol|
      if symbol.class != Symbol
        symbol = symbol.to_str.intern
      end
      if rest.has_key? symbol
        raise ArgumentError.new("duplicate member: #{symbol}");
      end
      rest[symbol] = true
    end
    members = rest.keys.freeze

    data_class = Class.new {
      self.define_singleton_method(:members) { members }

      define_method(:members) { self.class.members }

      define_method(:initialize) do |*values, **kwargs|
        local_members = members
        if kwargs && !kwargs.empty?
          remaining_members = members.dup
          unknown_members = nil
          values = []
          kwarg_values = kwargs.values
          kwargs.keys.map(&:to_sym).each do |key|
            idx = members.index(key)
            if idx == nil
              unknown_members ||= []
              unknown_members << key
              next
            end
            values[idx] = kwarg_values[idx]
            remaining_members.delete(key)
          end
          if !remaining_members.empty?
            raise ArgumentError.new((remaining_members.size == 1 ? "missing keyword: " : "missing keywords: ") + remaining_members.map(&:inspect).join(", "))
          end
          if unknown_members
            raise ArgumentError.new((unknown_members.size == 1 ? "unknown keyword: " : "unknown keywords: ") + unknown_members.map(&:inspect).join(", "))
          end
        end
        i = 0
        while i < local_members.size
          if i >= values.size
            raise ArgumentError.new((local_members.size - i == 1 ? "missing keyword: " : "missing keywords: ") + local_members[i..-1].map(&:inspect).join(", "))
          end
          member = local_members[i]
          var = :"@#{member}"
          instance_variable_set(var, values[i])
          i += 1
        end
        freeze
        nil
      end

      define_method(:to_h) do
        hash = Hash.new.compare_by_identity
        members.each {|member| hash[member] = instance_variable_get(:"@#{member}")}
        hash
      end

      define_method(:with) do |**kwargs|
        if !kwargs || kwargs.empty?
          return self
        end
        new_kwargs = {}.compare_by_identity
        kwargs.each {|k, v| new_kwargs[k.to_sym] = v}
        new_kwargs = to_h.merge(new_kwargs)
        self.class.new(**new_kwargs)
      end
      class << self; alias [] new; end
      members.each {|member| attr_reader(member)}
    }
    data_class.class_eval(&) if block_given?
    data_class
  end
end