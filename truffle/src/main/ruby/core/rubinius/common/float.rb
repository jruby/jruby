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

# Only part of Rubinius' float.rb

class Float < Numeric

  def coerce(other)
    return [other, self] if other.kind_of? Float
    [Float(other), self]
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
    elsif negative?
      Math::PI
    else
      0
    end
  end
  alias_method :angle, :arg
  alias_method :phase, :arg

  def negative?
    Rubinius.primitive :float_negative
    raise PrimitiveFailure, "Float#negative primitive failed"
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

  alias_method :quo, :/
  alias_method :modulo, :%

  def finite?
    not(nan? or infinite?)
  end

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

  # MODIFIED name to call from java
  def round_internal(ndigits=0)
    ndigits = Rubinius::Type.coerce_to(ndigits, Integer, :to_int)

    if ndigits == 0
      return Rubinius.invoke_primitive :float_round, self
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
    Rubinius.invoke_primitive(:float_round, self * f) / f.to_f
  end

  def dtoa
    Rubinius.primitive :float_dtoa
    raise PrimitiveFailure, "Fload#dtoa primitive failed"
  end

end
