# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Beware, RubyDebutTest use hard-coded line numbers from this file!

def fac(n)
  if n <= 1
    1
  else
    nMinusOne = n - 1
    nMOFact = fac(nMinusOne)
    res = n * nMOFact
    res
  end
end

def main
  res = fac(2)
  res
end
Truffle::Interop.export_method(:main)
