# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Float

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

end
