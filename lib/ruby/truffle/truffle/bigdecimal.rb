class Truffle::BigDecimal < Numeric
  include Comparable

  alias_method :eql?, :==
end

BigDecimal = Truffle::BigDecimal

module Kernel
  def BigDecimal(*args)
    BigDecimal.new *args
  end
end

require 'bigdecimal/math'
