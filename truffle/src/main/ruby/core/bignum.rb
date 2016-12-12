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

class Bignum < Integer

  def coerce(other)
    # NOTE (eregon, 16 Feb. 2015): In other implementations, other is converted to a Bignum here,
    # even if it fits in a Fixnum. We avoid it for implementation sanity
    # and to keep the representations strictly distinct over the range of values.
    # This is assumed for instance in Fixnum#/ and other places.
    # Note 2.4 solves this problem with Integer unification.
    raise TypeError unless other.is_a?(Integer)
    [other, self]
  end

  def <=>(other)
    Truffle.primitive :bignum_compare

    # We do not use redo_compare here because Ruby does not
    # raise if any part of the coercion or comparison raises
    # an exception.
    begin
      coerced = other.coerce self
      return nil unless coerced
      coerced[0] <=> coerced[1]
    rescue
      return nil
    end
  end

  def eql?(value)
    value.is_a?(Bignum) && self == value
  end

  def fdiv(n)
    to_f / n
  end

  def **(o)
    Truffle.primitive :bignum_pow

    if o.is_a?(Float) && self < 0 && o != o.round
      return Complex.new(self, 0) ** o
    elsif o.is_a?(Integer) && o < 0
      return Rational.new(self, 1) ** o
    end

    redo_coerced :**, o
  end
end
