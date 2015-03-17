class Object
  # The following helpers provide a level of indirection for running the specs
  # against a Hash implementation that has a different name than Hash.

  # Returns the Hash class.
  unless method_defined?(:hash_class)
    def hash_class
      Hash
    end
  end

  # Returns a new instance of hash_class.
  def new_hash(*args, &block)
    if block
      hash_class.new(&block)
    elsif args.size == 1
      value = args.first
      if value.is_a?(Hash) or value.is_a?(hash_class)
        hash_class[value]
      else
        hash_class.new value
      end
    else
      hash_class[*args]
    end
  end
end
