#########################################
# bench_math.rb
#
# Benchmark suite for the Math methods.
#########################################
require "benchmark"

MAX = 100000

Benchmark.bm(35) do |x|
   # ACOS
   x.report("Math.acos(0)"){
      MAX.times{ Math.acos(0) }
   }
   x.report("Math.acos(1)"){
      MAX.times{ Math.acos(1) }
   }
   x.report("Math.acos(-1)"){
      MAX.times{ Math.acos(-1) }
   }

   # ACOSH
   x.report("Math.acosh(0)"){
      MAX.times{ Math.acos(0) }
   }
   x.report("Math.acosh(1)"){
      MAX.times{ Math.acos(1) }
   }
   x.report("Math.acosh(-1)"){
      MAX.times{ Math.acos(-1) }
   }

   # ASIN
   x.report("Math.asin(0)"){
      MAX.times{ Math.asin(0) }
   }
   x.report("Math.asin(1)"){
      MAX.times{ Math.asin(1) }
   }
   x.report("Math.asin(-1)"){
      MAX.times{ Math.asin(-1) }
   }

   # ASINH
   x.report("Math.asinh(0)"){
      MAX.times{ Math.asinh(0) }
   }
   x.report("Math.asinh(1)"){
      MAX.times{ Math.asinh(1) }
   }
   x.report("Math.asinh(-1)"){
      MAX.times{ Math.asinh(-1) }
   }

   # ATAN
   x.report("Math.atan(0)"){
      MAX.times{ Math.atan(0) }
   }
   x.report("Math.atan(1)"){
      MAX.times{ Math.atan(1) }
   }
   x.report("Math.atan(-1)"){
      MAX.times{ Math.atan(-1) }
   }

   # ATANH
   x.report("Math.atanh(0)"){
      MAX.times{ Math.atanh(0) }
   }
   x.report("Math.atanh(1)"){
      MAX.times{ Math.atanh(1) }
   }
   x.report("Math.atanh(-1)"){
      MAX.times{ Math.atanh(-1) }
   }
end
