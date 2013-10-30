require 'bigdecimal'
require 'bigdecimal/util'

#
#--
# Contents:
#   sqrt(x, prec)
#   sin (x, prec)
#   cos (x, prec)
#   atan(x, prec)  Note: |x|<1, x=0.9999 may not converge.
#   PI  (prec)
#   E   (prec) == exp(1.0,prec)
#
# where:
#   x    ... BigDecimal number to be computed.
#            |x| must be small enough to get convergence.
#   prec ... Number of digits to be obtained.
#++
#
# Provides mathematical functions.
#
# Example:
#
#   require "bigdecimal/math"
#
#   include BigMath
#
#   a = BigDecimal((PI(100)/2).to_s)
#   puts sin(a,100) # => 0.10000000000000000000......E1
#
module BigMath
  module_function

  def zero
    BigDecimal('0')
  end

  def one
    BigDecimal('1')
  end

  def two
    BigDecimal('2')
  end

  # call-seq:
  #   sqrt(decimal, numeric) -> BigDecimal
  #
  # Computes the square root of +decimal+ to the specified number of digits of
  # precision, +numeric+.
  #
  #   BigMath::sqrt(BigDecimal.new('2'), 16).to_s
  #   #=> "0.14142135623730950488016887242096975E1"
  #
  def sqrt(x, prec)
    x.sqrt(prec)
  end

  # call-seq:
  #   sin(decimal, numeric) -> BigDecimal
  #
  # Computes the sine of +decimal+ to the specified number of digits of
  # precision, +numeric+.
  #
  # If +decimal+ is Infinity or NaN, returns NaN.
  #
  #   BigMath::sin(BigMath::PI(5)/4, 5).to_s
  #   #=> "0.70710678118654752440082036563292800375E0"
  #
  def sin(x, prec)
    raise ArgumentError, "Zero or negative precision for sin" if prec <= 0
    return BigDecimal("NaN") if x.infinite? || x.nan?
    n    = prec + BigDecimal.double_fig
    one  = BigDecimal("1")
    two  = BigDecimal("2")
    x = -x if neg = x < 0
    if x > (twopi = two * BigMath.PI(prec))
      if x > 30
        x %= twopi
      else
        x -= twopi while x > twopi
      end
    end
    x1   = x
    x2   = x.mult(x,n)
    sign = 1
    y    = x
    d    = y
    i    = one
    z    = one
    while d.nonzero? && ((m = n - (y.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      sign = -sign
      x1  = x2.mult(x1,n)
      i  += two
      z  *= (i-one) * i
      d   = sign * x1.div(z,m)
      y  += d
    end
    neg ? -y : y
  end

  # call-seq:
  #   cos(decimal, numeric) -> BigDecimal
  #
  # Computes the cosine of +decimal+ to the specified number of digits of
  # precision, +numeric+.
  #
  # If +decimal+ is Infinity or NaN, returns NaN.
  #
  #   BigMath::cos(BigMath::PI(4), 16).to_s
  #   #=> "-0.999999999999999999999999999999856613163740061349E0"
  #
  def cos(x, prec)
    raise ArgumentError, "Zero or negative precision for cos" if prec <= 0
    return BigDecimal("NaN") if x.infinite? || x.nan?
    n    = prec + BigDecimal.double_fig
    one  = BigDecimal("1")
    two  = BigDecimal("2")
    x = -x if x < 0
    if x > (twopi = two * BigMath.PI(prec))
      if x > 30
        x %= twopi
      else
        x -= twopi while x > twopi
      end
    end
    x1 = one
    x2 = x.mult(x,n)
    sign = 1
    y = one
    d = y
    i = BigDecimal("0")
    z = one
    while d.nonzero? && ((m = n - (y.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      sign = -sign
      x1  = x2.mult(x1,n)
      i  += two
      z  *= (i-one) * i
      d   = sign * x1.div(z,m)
      y  += d
    end
    y
  end

  # call-seq:
  #   atan(decimal, numeric) -> BigDecimal
  #
  # Computes the arctangent of +decimal+ to the specified number of digits of
  # precision, +numeric+.
  #
  # If +decimal+ is NaN, returns NaN.
  #
  #   BigMath::atan(BigDecimal.new('-1'), 16).to_s
  #   #=> "-0.785398163397448309615660845819878471907514682065E0"
  #
  def atan(x, prec)
    raise ArgumentError, "Zero or negative precision for atan" if prec <= 0
    return BigDecimal("NaN") if x.nan?
    pi = PI(prec)
    x = -x if neg = x < 0
    return pi.div(neg ? -2 : 2, prec) if x.infinite?
    return pi / (neg ? -4 : 4) if x.round(prec) == 1
    x = BigDecimal("1").div(x, prec) if inv = x > 1
    x = (-1 + sqrt(1 + x**2, prec))/x if dbl = x > 0.5
    n    = prec + BigDecimal.double_fig
    y = x
    d = y
    t = x
    r = BigDecimal("3")
    x2 = x.mult(x,n)
    while d.nonzero? && ((m = n - (y.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      t = -t.mult(x2,n)
      d = t.div(r,m)
      y += d
      r += 2
    end
    y *= 2 if dbl
    y = pi / 2 - y if inv
    y = -y if neg
    y
  end

  # call-seq:
  #   PI(numeric) -> BigDecimal
  #
  # Computes the value of pi to the specified number of digits of precision,
  # +numeric+.
  #
  #   BigMath::PI(10).to_s
  #   #=> "0.3141592653589793238462643388813853786957412E1"
  #
  def PI(prec)
    raise ArgumentError, "Zero or negative argument for PI" if prec <= 0
    n      = prec + BigDecimal.double_fig
    zero   = BigDecimal("0")
    one    = BigDecimal("1")
    two    = BigDecimal("2")

    m25    = BigDecimal("-0.04")
    m57121 = BigDecimal("-57121")

    pi     = zero

    d = one
    k = one
    t = BigDecimal("-80")
    while d.nonzero? && ((m = n - (pi.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      t   = t*m25
      d   = t.div(k,m)
      k   = k+two
      pi  = pi + d
    end

    d = one
    k = one
    t = BigDecimal("956")
    while d.nonzero? && ((m = n - (pi.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      t   = t.div(m57121,n)
      d   = t.div(k,m)
      pi  = pi + d
      k   = k+two
    end
    pi
  end

  # call-seq:
  #   E(numeric) -> BigDecimal
  #
  # Computes e (the base of natural logarithms) to the specified number of
  # digits of precision, +numeric+.
  #
  #   BigMath::E(10).to_s
  #   #=> "0.271828182845904523536028752390026306410273E1"
  #
  def E(prec)
    raise ArgumentError, "Zero or negative precision for E" if prec <= 0
    n    = prec + BigDecimal.double_fig
    one  = BigDecimal("1")
    y  = one
    d  = y
    z  = one
    i  = 0
    while d.nonzero? && ((m = n - (y.exponent - d.exponent).abs) > 0)
      m = BigDecimal.double_fig if m < BigDecimal.double_fig
      i += 1
      z *= i
      d  = one.div(z,m)
      y += d
    end
    y
  end

  # call-seq:
  #   log(decimal, numeric) -> BigDecimal
  #
  # Computes the natural logarithm of +decimal+ to the specified number of
  # digits of precision, +numeric+.
  #
  # If +decimal+ is zero of negative raise Math::DomainError.
  #
  # If +decimal+ is positive infinity, returns Infinity.
  #
  # If +decimal+ is NaN, returns NaN.
  #
  #   BigMath::log(BigMath::E(10), 10).to_s
  #   #=> "1.000000000000"
  #
  def log(x, precision)
    raise ArgumentError if x.nil?
    raise Math::DomainError if x.is_a?(Complex)
    raise Math::DomainError if x <= 0
    raise ArgumentError unless precision.is_a?(Integer)
    raise ArgumentError if precision < 1
    return BigDecimal::INFINITY if x == BigDecimal::INFINITY
    return BigDecimal::NAN if x.is_a?(BigDecimal) && x.nan?
    return BigDecimal::NAN if x.is_a?(Float) && x.nan?

    # this uses the series expansion of the Arctangh (Arc tangens hyperbolicus)
    # http://en.wikipedia.org/wiki/Area_hyperbolic_tangent
    # where ln(x) = 2 * artanh ((x - 1) / (x + 1))
    # d are the elements in the series (getting smaller and smaller)

    x = x.to_d
    n = precision + BigDecimal.double_fig
    z = (x - one) / (x + one)
    z2 = z * z

    series_sum = z
    series_element = z

    i = 1
    while series_element != 0 do
      sum_exponent = series_sum.exponent
      element_exponent = series_element.exponent
      remaining_precision = n - (sum_exponent - element_exponent).abs
      break if remaining_precision < 0
      z = z * z2
      i += 2
      series_element = z / i
      series_sum += series_element
    end

    two * series_sum
  end

=begin
BigMath_s_log(VALUE klass, VALUE x, VALUE vprec)
{

   expo = VpExponent10(vx);
   if (expo < 0 || expo >= 3) {
     char buf[16];
     snprintf(buf, 16, "1E%"PRIdVALUE, -expo);
     x = BigDecimal_mult2(x, ToValue(VpCreateRbObject(1, buf)), vn);
   }
  else {
 expo = 0;
 }

# This is for later (recursive when out of bound)
   if (expo != 0) {
     VALUE log10, vexpo, dy;
     log10 = BigMath_s_log(klass, INT2FIX(10), vprec);
     vexpo = ToValue(GetVpValue(SSIZET2NUM(expo), 1));
     dy = BigDecimal_mult(log10, vexpo);
     y = BigDecimal_add(y, dy);
   }
=end

end
