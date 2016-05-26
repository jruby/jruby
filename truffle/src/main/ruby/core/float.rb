# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Float < Numeric

  # Floats are also immediate values for us, they are primitive double
  include ImmediateValue

  NAN        = 0.0 / 0.0
  INFINITY   = 1.0 / 0.0
  EPSILON    = 2.2204460492503131e-16
  RADIX      = 2
  ROUNDS     = 1
  MIN        = 2.2250738585072014e-308
  MAX        = 1.7976931348623157e+308
  MIN_EXP    = -1021
  MAX_EXP    = 1024
  MIN_10_EXP = -307
  MAX_10_EXP = 308
  DIG        = 15
  MANT_DIG   = 53

  # for Float ** Rational we would normally do Rational.convert(a) ** b, but
  # this ends up being recursive using Rubinius' code, so we use this helper
  # instead.

  def pow_rational(rational)
    self ** rational.to_f
  end

  private :pow_rational

  def equal_fallback(other)
    # Fallback from Rubinius' Float#==, after the primitive call

    begin
      b, a = math_coerce(other)
      return a == b
    rescue TypeError
      return other == self
    end
  end

  private :equal_fallback

  FFI = Rubinius::FFI

  def self.induced_from(obj)
    case obj
    when Float, Bignum, Fixnum
      obj.to_f
    else
      raise TypeError, "failed to convert #{obj.class} into Float"
    end
  end

  def **(other)
    Truffle.primitive :float_pow

    if other.is_a?(Float) && self < 0 && other != other.round
      return Complex.new(self, 0) ** other
    end

    b, a = math_coerce other
    a ** b
  end

  def imaginary
    0
  end

  def numerator
    if nan?
      NAN
    elsif infinite? == 1
      INFINITY
    elsif infinite? == -1
      -INFINITY
    else
      super
    end
  end

  def denominator
    if infinite? || nan?
      1
    else
      super
    end
  end

  def to_r
    f, e = Math.frexp self
    f = Math.ldexp(f, MANT_DIG).to_i
    e -= MANT_DIG

    (f * (RADIX ** e)).to_r
  end

  def arg
    if nan?
      self
    elsif signbit?
      Math::PI
    else
      0
    end
  end
  alias_method :angle, :arg
  alias_method :phase, :arg

  def rationalize(eps=undefined)
    if undefined.equal?(eps)
      f, n = Math.frexp self
      f = Math.ldexp(f, Float::MANT_DIG).to_i
      n -= Float::MANT_DIG

      Rational.new(2 * f, 1 << (1 - n)).rationalize(Rational.new(1, 1 << (1 - n)))
    else
      to_r.rationalize(eps)
    end
  end

  def round(ndigits=0)
    ndigits = Rubinius::Type.coerce_to(ndigits, Integer, :to_int)

    if ndigits == 0
      return Truffle.invoke_primitive :float_round, self
    elsif ndigits < 0
      return truncate.round ndigits
    end

    return self if infinite? or nan?

    _, exp = Math.frexp(self)

    if ndigits >= (Float::DIG + 2) - (exp > 0 ? exp / 4 : exp / 3 - 1)
      return self
    end

    if ndigits < -(exp > 0 ? exp / 3 + 1 : exp / 4)
      return 0.0
    end

    f = 10**ndigits
    Truffle.invoke_primitive(:float_round, self * f) / f.to_f
  end
  def coerce(other)
    return [other, self] if other.kind_of? Float
    [Float(other), self]
  end

  def -@
    Truffle.primitive :float_neg
    raise PrimitiveFailure, "Float#-@ primitive failed"
  end

  def abs
    FFI::Platform::Math.fabs(self)
  end

  alias_method :magnitude, :abs

  def signbit?
    Truffle.primitive :float_signbit_p
    raise PrimitiveFailure, "Float#signbit? primitive failed"
  end

  def +(other)
    Truffle.primitive :float_add
    b, a = math_coerce other
    a + b
  end

  def -(other)
    Truffle.primitive :float_sub
    b, a = math_coerce other
    a - b
  end

  def *(other)
    Truffle.primitive :float_mul
    b, a = math_coerce other
    a * b
  end

  #--
  # see README-DEVELOPERS regarding safe math compiler plugin
  #++

  def divide(other)
    Truffle.primitive :float_div
    redo_coerced :/, other
  end

  alias_method :quo, :/
  alias_method :fdiv, :/

  INFINITY = 1.0 / 0.0
  NAN = 0.0 / 0.0

  def divmod(other)
    Truffle.primitive :float_divmod
    b, a = math_coerce other
    a.divmod b
  end

  def %(other)
    return 0 / 0.to_f if other == 0
    Truffle.primitive :float_mod
    b, a = math_coerce other
    a % b
  end

  alias_method :modulo, :%

  def <(other)
    Truffle.primitive :float_lt
    b, a = math_coerce other, :compare_error
    a < b
  end

  def <=(other)
    Truffle.primitive :float_le
    b, a = math_coerce other, :compare_error
    a <= b
  end

  def >(other)
    Truffle.primitive :float_gt
    b, a = math_coerce other, :compare_error
    a > b
  end

  def >=(other)
    Truffle.primitive :float_ge
    b, a = math_coerce other, :compare_error
    a >= b
  end

  def <=>(other)
    Truffle.primitive :float_compare
    b, a = math_coerce other, :compare_error
    a <=> b
  rescue ArgumentError
    nil
  end

  def ==(other)
    Truffle.primitive :float_equal
    begin
      b, a = math_coerce(other)
      return a == b
    rescue TypeError
      return other == self
    end
  end

  def eql?(other)
    Truffle.primitive :float_eql
    false
  end

  def nan?
    Truffle.primitive :float_isnan
    raise PrimitiveFailure, "Float#nan? primitive failed"
  end

  def infinite?
    Truffle.primitive :float_isinf
    raise PrimitiveFailure, "Float#infinite? primitive failed"
  end

  def finite?
    not (nan? or infinite?)
  end

  def to_f
    self
  end

  def to_i
    Truffle.primitive :float_to_i
    raise PrimitiveFailure, "Float#to_i primitive failed"
  end

  alias_method :to_int, :to_i
  alias_method :truncate, :to_i

  def to_s
    to_s_minimal
  end

  alias_method :inspect, :to_s

  def to_s_minimal
    Truffle.primitive :float_to_s_minimal
    raise PrimitiveFailure, "Float#to_s_minimal primitive failed: output exceeds buffer size"
  end

  def to_s_formatted(fmt)
    Truffle.primitive :float_to_s_formatted
    raise PrimitiveFailure, "Float#to_s_formatted primitive failed: output exceeds buffer size"
  end
  private :to_s_formatted

  def dtoa
    Truffle.primitive :float_dtoa
    raise PrimitiveFailure, "Fload#dtoa primitive failed"
  end

  def to_packed(size)
    Truffle.primitive :float_to_packed
    raise PrimitiveFailure, "Float#to_packed primitive failed"
  end

  def ceil
    int = to_i()

    return int if self == int or self < 0
    return int + 1
  end

  def floor
    int = to_i()

    return int if self > 0 or self == int
    return int - 1
  end
end
