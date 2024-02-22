module BigMath
  module_function

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
  def log(x, prec)
    raise ArgumentError.new("#{x.inspect} can't be coerced into BigDecimal") if x.nil? || !x.is_a?(Numeric)
    raise Math::DomainError.new("#{x.class} argument for BigDecimal.log") if x.is_a?(Complex)
    raise Math::DomainError.new("Zero or negative argument for BigDecimal.log") if x <= 0
    raise ArgumentError unless (true if Integer(prec) rescue false)
    prec = prec.to_i
    raise ArgumentError if prec < 1
    if x.is_a?(BigDecimal) || x.is_a?(Float)
      return BigDecimal::INFINITY if x.infinite?
      return BigDecimal::NAN if x.nan?
    end
    x = x.is_a?(Rational) ? BigDecimal(x, prec) : x.is_a?(Float) ? BigDecimal(x, Float::DIG) : BigDecimal(x)
    return BigDecimal::INFINITY if x.infinite?
    return BigDecimal::NAN if x.nan?

    # this uses the series expansion of the Arctangh (Arc tangens hyperbolicus)
    # http://en.wikipedia.org/wiki/Area_hyperbolic_tangent
    # where ln(x) = 2 * artanh ((x - 1) / (x + 1))
    # d are the elements in the series (getting smaller and smaller)

    rmpd_double_figures = 16 # from MRI ruby
    n = prec + rmpd_double_figures

    # offset the calculation to the efficient (0.1)...(10) window
    expo = x.exponent
    use_window = (x > 10) || (expo < 0) # allow up to 10 itself
    if use_window
      offset = BigDecimal("1E#{-expo}")
      x = x.mult(offset, n)
    end

    z = (x - 1).div((x + 1), n)
    z2 = z.mult(z, n)
    series_sum = z
    series_element = z

    i = 1
    while series_element.nonzero? do
      sum_exponent = series_sum.exponent
      element_exponent = series_element.exponent
      remaining_precision = n - (sum_exponent - element_exponent).abs
      break if remaining_precision < 0
      if remaining_precision < rmpd_double_figures
        remaining_precision = rmpd_double_figures
      end
      z = z.mult(z2, n)
      i += 2
      series_element = z.div(i, remaining_precision)
      series_sum += series_element
    end

    window_result = series_sum * 2

    # reset the result back to the original value if needed
    if use_window
      log10 = log(10, n)
      window_result + log10.mult(expo, n)
    else
      window_result
    end
  end

  def exp(x, prec)
    raise ArgumentError.new("#{x.inspect} can't be coerced into BigDecimal") if x.nil? || !x.is_a?(Numeric)
    raise ArgumentError.new("#{x.class} can't be coerced into BigDecimal") if x.is_a?(Complex)
    raise ArgumentError unless (true if Integer(prec) rescue false)
    prec = prec.to_i
    raise ArgumentError if prec < 1
    return BigDecimal::NAN if x.is_a?(BigDecimal) && x.nan?
    return BigDecimal::NAN if x.is_a?(Float) && x.nan?
    x = x.is_a?(Rational) ? BigDecimal(x, Float::DIG) : x.is_a?(Float) ? BigDecimal(x, Float::DIG) : BigDecimal(x)
    return BigDecimal::NAN if x.nan?
    return x.sign > 0 ? BigDecimal::INFINITY : BigDecimal(0, prec) if x.infinite?

    df = BigDecimal.double_fig
    n = prec + df
    one = BigDecimal(1)
    x = -x if neg = x.sign < 0

    y = one;
    d = y;
    i = 1;

    while d.nonzero? && ((m = n - (y.exponent - d.exponent).abs) > 0)
      m = df if m < df
      d = d.mult(x, n)
      d = d.div(i, m)
      y += d
      i += 1;
    end
    if neg
      one.div(y, prec)
    else
      y.round(prec - y.exponent)
    end
  end
end