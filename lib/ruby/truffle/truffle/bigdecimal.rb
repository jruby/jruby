class Truffle::BigDecimal < Numeric
  include Comparable

  BASE = 10_000

  SIGN_NEGATIVE_INFINITE = -3
  SIGN_NEGATIVE_FINITE   = -2
  SIGN_NEGATIVE_ZERO     = -1
  SIGN_NaN               = 0
  SIGN_POSITIVE_ZERO     = 1
  SIGN_POSITIVE_FINITE   = 2
  SIGN_POSITIVE_INFINITE = 3

  EXCEPTION_INFINITY   = 1
  EXCEPTION_OVERFLOW   = 1
  EXCEPTION_NaN        = 2
  EXCEPTION_UNDERFLOW  = 4
  EXCEPTION_ZERODIVIDE = 16
  EXCEPTION_ALL        = 255
  ROUND_MODE           = 256

  ROUND_UP        = 1
  ROUND_DOWN      = 2
  ROUND_HALF_UP   = 3
  ROUND_HALF_DOWN = 4
  ROUND_CEILING   = 5
  ROUND_FLOOR     = 6
  ROUND_HALF_EVEN = 7

  def self.mode(key, value = nil)
    raise ArgumentError, 'requires key to be Fixnum' unless key.is_a? Fixnum
    if key == ROUND_MODE
      Thread.current[:'BigDecimal.rounding_mode'] ||= 3
      if value
        Thread.current[:'BigDecimal.rounding_mode'] = value
      else
        Thread.current[:'BigDecimal.rounding_mode']
      end
    else
      Thread.current[:'BigDecimal.exception_mode'] ||= 0
      case value
        when true
          Thread.current[:'BigDecimal.exception_mode'] |= key
          return value
        when false
          Thread.current[:'BigDecimal.exception_mode'] &= ~key
          return value
        when nil
          # FIXME (pitr 20-Jun-2015): CRuby always returns BigDecimal.exception_mode internal value ignoring the key
          return Thread.current[:'BigDecimal.exception_mode'] & key == key
      end
    end
  end

  def self.limit(limit = nil)
    Thread.current[:'BigDecimal.precision_limit'] ||= 0
    if limit
      raise ArgumentError, 'requires key to be Fixnum' unless limit.is_a? Fixnum
      old                                           = Thread.current[:'BigDecimal.precision_limit']
      Thread.current[:'BigDecimal.precision_limit'] = limit
      old
    else
      Thread.current[:'BigDecimal.precision_limit']
    end
  end

  # TODO (pitr 20-jun-2015): remove when lazy setup is added
  def self.name
    'BigDecimal'
  end

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

  def split
    sign = self.sign
    sign = 1 if sign > 1
    sign = -1 if sign < -1
    [sign, unscaled, 10, exponent]
  end

  def floor(digit = nil)
    if digit
      round digit, ROUND_FLOOR
    else
      rounded = round 0, ROUND_FLOOR
      integer = rounded.to_i
      return rounded == integer ? integer : rounded
    end
  end

  def ceil(digit = nil)
    if digit
      round digit, ROUND_CEILING
    else
      rounded = round 0, ROUND_CEILING
      integer = rounded.to_i
      return rounded == integer ? integer : rounded
    end
  end
end

BigDecimal = Truffle::BigDecimal

module Kernel
  def BigDecimal(*args)
    BigDecimal.new *args
  end
end

require 'bigdecimal/math'
