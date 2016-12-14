# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Math

  PI = 3.14159265358979323846
  E = 2.7182818284590452354
  
  DomainError = Errno::EDOM

  module_function

  def frexp(x)
    Truffle.primitive :math_frexp
    frexp Rubinius::Type.coerce_to_float(x)
  end

  def ldexp(fraction, exponent)
    Truffle.primitive :math_ldexp
    raise RangeError, "float NaN out of range of integer" if Float === exponent and exponent.nan?
    ldexp(
      Rubinius::Type.coerce_to_float(fraction),
      Rubinius::Type.coerce_to_int(exponent))
  end

end
