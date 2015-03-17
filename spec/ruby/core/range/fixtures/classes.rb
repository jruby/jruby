module RangeSpecs
  class TenfoldSucc
    include Comparable

    attr_reader :n

    def initialize(n)
      @n = n
    end

    def <=>(other)
      @n <=> other.n
    end

    def succ
      self.class.new(@n * 10)
    end
  end
end