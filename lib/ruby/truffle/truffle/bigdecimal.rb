class Truffle::BigDecimal < Numeric
  include Comparable

  BASE = 10_000

  SIGN_NEGATIVE_INFINITE = -3
  SIGN_NEGATIVE_FINITE   = -2
  SIGN_NEGATIVE_ZERO     = -1
  SIGN_NaN               =  0
  SIGN_POSITIVE_ZERO     =  1
  SIGN_POSITIVE_FINITE   =  2
  SIGN_POSITIVE_INFINITE =  3

  def ==(o)
    (self <=> o) == 0 rescue false
  end

  alias_method :eql?, :==
  alias_method :===, :==

  def coerce(other)
    [BigDecimal(other), self]
  end

  # TODO (pitr 28-may-2015): compare with pure Java versions
  def >(other)
    unless comp = (self <=> other)
      return false if nan? || (BigDecimal === other && other.nan?)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    comp > 0
  end

  def >=(other)
    unless comp = (self <=> other)
      return false if nan? || (BigDecimal === other && other.nan?)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    comp >= 0
  end

  def <(other)
    unless comp = (self <=> other)
      return false if nan? || (BigDecimal === other && other.nan?)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    comp < 0
  end

  def <=(other)
    unless comp = (self <=> other)
      return false if nan? || (BigDecimal === other && other.nan?)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    comp <= 0
  end

  def nonzero?
    zero? ? nil : self
  end
end

BigDecimal = Truffle::BigDecimal

module Kernel
  def BigDecimal(*args)
    BigDecimal.new *args
  end
end

require 'bigdecimal/math'
