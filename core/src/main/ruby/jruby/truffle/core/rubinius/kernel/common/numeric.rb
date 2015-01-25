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

# Only part of Rubinius' numeric.rb

class Numeric

  def eql?(other)
    return false unless other.instance_of? self.class
    self == other
  end

  #--
  # We deviate from MRI behavior here because we ensure that Fixnum op Bignum
  # => Bignum (possibly normalized to Fixnum)
  #
  # Note these differences on MRI, where a is a Fixnum, b is a Bignum
  #
  #   a.coerce b => [Float, Float]
  #   b.coerce a => [Bignum, Bignum]
  #++

  def abs
    self < 0 ? -self : self
  end

  def coerce(other)
    if other.instance_of? self.class
      return [other, self]
    end

    [Float(other), Float(self)]
  end

  ##
  # This method mimics the semantics of MRI's do_coerce function
  # in numeric.c. Note these differences between it and #coerce:
  #
  #   1.2.coerce("2") => [2.0, 1.2]
  #   1.2 + "2" => TypeError: String can't be coerced into Float
  #
  # See also Integer#coerce

  def math_coerce(other, error=:coerce_error)
    begin
      values = other.coerce(self)
    rescue
      if error == :coerce_error
        raise TypeError, "#{other.class} can't be coerced into #{self.class}"
      else
        raise ArgumentError, "comparison of #{self.class} with #{other.class} failed"
      end
    end

    unless Rubinius::Type.object_kind_of?(values, Array) && values.length == 2
      raise TypeError, "coerce must return [x, y]"
    end

    return values[1], values[0]
  end
  private :math_coerce

  def redo_coerced(meth, right)
    b, a = math_coerce(right)
    a.__send__ meth, b
  end
  private :redo_coerced

  def zero?
    self == 0
  end

  def nonzero?
    zero? ? nil : self
  end

  def div(other)
    raise ZeroDivisionError, "divided by 0" if other == 0
    self.__slash__(other).floor
  end

  def fdiv(other)
    self.to_f / other
  end

  alias_method :magnitude, :abs

  def real?
    true
  end

  def numerator
    to_r.numerator
  end
  
  def denominator
    to_r.denominator
  end

end
