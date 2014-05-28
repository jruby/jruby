module ReturnSpecs
  class MethodWithBlock
    def method1
      return [2, 3].inject 0 do |a, b|
        a + b
      end
      nil
    end

    def get_ary(count, &blk)
      count.times.to_a do |i|
        blk.call(i) if blk
      end
    end

    def method2
      return get_ary 3 do |i|
      end
      nil
    end
  end
end
