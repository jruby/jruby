module KernelSpecs
  class SingletonMethod
    def self.singleton_method_added name
      ScratchPad.record [:method_added, name]
    end

    def self.singleton_method_removed name
      ScratchPad.record [:method_removed, name]
    end

    def self.singleton_method_undefined name
      ScratchPad.record [:method_undefined, name]
    end

    def self.singleton_method_to_remove
    end

    def self.singleton_method_to_undefine
    end

  end
end
