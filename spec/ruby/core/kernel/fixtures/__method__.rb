module KernelSpecs
  class MethodTest
    def f
      __method__
    end

    alias_method :g, :f

    def in_block
      (1..2).map { __method__ }
    end

    define_method(:dm) do
      __method__
    end

    define_method(:dm_block) do
      (1..2).map { __method__ }
    end

    def from_eval
      eval "__method__"
    end
  end
end
