# TODO require bigdecimal/*

BigDecimal = Truffle::BigDecimal

module Kernel
  def BigDecimal(*args)
    BigDecimal.new *args
  end
end
