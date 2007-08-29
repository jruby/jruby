#!/usr/bin/ruby
#  The Great Computer Language Shootout
#  http://shootout.alioth.debian.org/
#
#  contributed by Gabriele Renzi 

def takfp x, y, z
  return z unless y < x
  takfp( takfp(x-1.0, y, z),
    takfp(y-1.0, z, x),
    takfp(z-1.0, x, y))
end

n=Float(ARGV[0])
puts takfp(n*3.0, n*2.0, n*1.0)
