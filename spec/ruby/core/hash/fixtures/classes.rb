module HashSpecs
  class MyHash < hash_class; end

  class MyInitializerHash < hash_class

    def initialize
      raise "Constructor called"
    end

  end

  class NewHash < hash_class
    def initialize(*args)
      args.each_with_index do |val, index|
        self[index] = val
      end
    end
  end

  class DefaultHash < hash_class
    def default(key)
      100
    end
  end

  class ToHashHash < hash_class
    def to_hash
      new_hash "to_hash" => "was", "called!" => "duh."
    end
  end

  class KeyWithPrivateHash
    private :hash
  end

  def self.empty_frozen_hash
    @empty ||= new_hash
    @empty.freeze
    @empty
  end

  def self.frozen_hash
    @hash ||= new_hash(1 => 2, 3 => 4)
    @hash.freeze
    @hash
  end
end
