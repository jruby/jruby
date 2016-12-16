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

#
#   complex.rb -
#   	$Release Version: 0.5 $
#   	$Revision: 1.3 $
#   	$Date: 1998/07/08 10:05:28 $
#   	by Keiju ISHITSUKA(SHL Japan Inc.)
#

class Complex < Numeric

  undef_method :%
  undef_method :<
  undef_method :<=
  undef_method :<=>
  undef_method :>
  undef_method :>=
  undef_method :between?
  undef_method :div
  undef_method :divmod
  undef_method :floor
  undef_method :ceil
  undef_method :modulo
  undef_method :remainder
  undef_method :round
  undef_method :step
  undef_method :truncate
  undef_method :i

  def self.convert(real, imag = undefined)
    if nil.equal?(real) || nil.equal?(imag)
      raise TypeError, "cannot convert nil into Complex"
    end
    imag = nil if undefined.equal?(imag)

    real = real.to_c if real.kind_of?(String)
    imag = imag.to_c if imag.kind_of?(String)

    if real.kind_of?(Complex) && !real.imag.kind_of?(Float) && real.imag == 0
      real = real.real
    end

    if imag.kind_of?(Complex) && !imag.imag.kind_of?(Float) && imag.imag == 0
      imag = imag.real
    end

    if real.kind_of?(Complex) && !imag.kind_of?(Float) && imag == 0
      return real
    end

    if imag.nil?
      if real.kind_of?(Numeric) && !real.real?
        return real
      elsif !real.kind_of?(Numeric)
        return Rubinius::Type.coerce_to(real, Complex, :to_c)
      else
        imag = 0
      end
    elsif real.kind_of?(Numeric) && imag.kind_of?(Numeric) && (!real.real? || !imag.real?)
      return real + imag * Complex.new(0, 1)
    end

    return real if Rubinius.mathn_loaded? && imag.equal?(0)
    rect(real, imag)
  end

  private_class_method :convert

  def Complex.generic?(other) # :nodoc:
    other.kind_of?(Integer) or
    other.kind_of?(Float) or
    (defined?(Rational) and other.kind_of?(Rational))
  end

  def Complex.rect(real, imag=0)
    raise TypeError, 'not a real' unless check_real?(real) && check_real?(imag)
    new(real, imag)
  end
  class << self; alias_method :rectangular, :rect end

  def Complex.polar(r, theta=0)
    raise TypeError, 'not a real' unless check_real?(r) && check_real?(theta)

    Complex(r*Math.cos(theta), r*Math.sin(theta))
  end

  def Complex.check_real?(obj)
    obj.kind_of?(Numeric) && obj.real?
  end
  private_class_method :check_real?

  def initialize(a, b = 0)
    @real = a
    @imag = b
  end

  def -@
    Complex(-real, -imag)
  end

  def +(other)
    if other.kind_of?(Complex)
      Complex(real + other.real, imag + other.imag)
    elsif other.kind_of?(Numeric) && other.real?
      Complex(real + other, imag)
    else
      redo_coerced(:+, other)
    end
  end

  def -(other)
    if other.kind_of?(Complex)
      Complex(real - other.real, imag - other.imag)
    elsif other.kind_of?(Numeric) && other.real?
      Complex(real - other, imag)
    else
      redo_coerced(:-, other)
    end
  end

  def *(other)
    if other.kind_of?(Complex)
      Complex(real * other.real - imag * other.imag,
              real * other.imag + imag * other.real)
    elsif other.kind_of?(Numeric) && other.real?
      Complex(real * other, imag * other)
    else
      redo_coerced(:*, other)
    end
  end

  def divide(other)
    if other.kind_of?(Complex)
      self * other.conjugate / other.abs2
    elsif other.kind_of?(Numeric) && other.real?
      Complex(real.quo(other), imag.quo(other))
    else
      redo_coerced(:quo, other)
    end
  end

  alias_method :/, :divide
  alias_method :quo, :divide

  def ** (other)
    if !other.kind_of?(Float) && other == 0
      return Complex(1)
    end
    if other.kind_of?(Complex)
      r, theta = polar
      ore = other.real
      oim = other.imag
      nr = Math.exp(ore*Math.log(r) - oim * theta)
      ntheta = theta*ore + oim*Math.log(r)
      Complex.polar(nr, ntheta)
    elsif other.kind_of?(Integer)
      if other > 0
        x = self
        z = x
        n = other - 1
        while n != 0
          while (div, mod = n.divmod(2)
           mod == 0)
            x = Complex(x.real*x.real - x.imag*x.imag, 2*x.real*x.imag)
            n = div
          end
          z *= x
          n -= 1
        end
        z
      else
        if defined? Rational
          (Rational.new(1, 1) / self) ** -other
        else
          self ** Float(other)
        end
      end
    elsif Complex.generic?(other)
      r, theta = polar
      Complex.polar(r**other, theta*other)
    else
      x, y = other.coerce(self)
      x**y
    end
  end

  def abs
    Math.hypot(@real, @imag)
  end

  alias_method :magnitude, :abs

  def abs2
    @real*@real + @imag*@imag
  end

  def arg
    Math.atan2(@imag, @real)
  end
  alias_method :angle, :arg
  alias_method :phase, :arg

  def polar
    [abs, arg]
  end

  def conjugate
    Complex(@real, -@imag)
  end
  alias conj conjugate

  def ==(other)
    if other.kind_of?(Complex)
      real == other.real && imag == other.imag
    elsif other.kind_of?(Numeric) && other.real?
      real == other && imag == 0
    else
      other == self
    end
  end

  def eql?(other)
    other.kind_of?(Complex) and
    imag.class == other.imag.class and
    real.class == other.real.class and
    self == other
  end

  def coerce(other)
    if other.kind_of?(Numeric) && other.real?
      [Complex.new(other, 0), self]
    elsif other.kind_of?(Complex)
      [other, self]
    else
      raise TypeError, "#{other.class} can't be coerced into Complex"
    end
  end

  def denominator
    @real.denominator.lcm(@imag.denominator)
  end

  def numerator
    cd = denominator
    Complex(@real.numerator*(cd/@real.denominator),
            @imag.numerator*(cd/@imag.denominator))
  end

  def real?
    false
  end

  def rect
    [@real, @imag]
  end

  alias_method :rectangular, :rect

  def to_f
    raise RangeError, "can't convert #{self} into Float" unless !imag.kind_of?(Float) && imag == 0
    real.to_f
  end

  def to_i
    raise RangeError, "can't convert #{self} into Integer" unless !imag.kind_of?(Float) && imag == 0
    real.to_i
  end

  def to_r
    raise RangeError, "can't' convert #{self} into Rational" unless !imag.kind_of?(Float) && imag == 0
    real.to_r
  end

  def rationalize(eps = nil)
    raise RangeError, "can't' convert #{self} into Rational" unless !imag.kind_of?(Float) && imag == 0
    real.rationalize(eps)
  end

  def to_s
    result = real.to_s

    if imag.kind_of?(Float) ? !imag.nan? && imag.signbit? : imag < 0
      result << "-"
    else
      result << "+"
    end

    imag_s = imag.abs.to_s
    result << imag_s

    unless imag_s[-1] =~ /\d/
      result << "*"
    end

    result << "i"
    result
  end

  def hash
    Truffle.invoke_primitive :fixnum_memhash, @real.hash, @imag.hash
  end

  def inspect
    "(#{to_s})"
  end

  def fdiv(other)
    raise TypeError, "#{other.class} can't be coerced into Complex" unless other.is_a?(Numeric)

    # FIXME
    self / other
  end

  def marshal_dump
    ary = [real, imag]
    instance_variables.each do |ivar|
      ary.instance_variable_set(ivar, instance_variable_get(ivar))
    end
    ary
  end

  private :marshal_dump

  def marshal_load(ary)
    @real, @imag = ary
    ary.instance_variables.each do |ivar|
      instance_variable_set(ivar, ary.instance_variable_get(ivar))
    end
    self
  end

  I = Complex(0, 1)

  attr_reader :real
  attr_reader :imag
  alias_method :imaginary, :imag

  private

  def real=(real)
    @real = real
  end

  def imag=(imag)
    @imag = imag
  end

  undef_method :negative?
  undef_method :positive?

end
