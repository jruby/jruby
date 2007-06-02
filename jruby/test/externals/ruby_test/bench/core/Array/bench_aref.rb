##############################################################
# bench_aref.rb
#
# Benchmark suite for the Array#[] method.
##############################################################
require "benchmark"

MAX = 100000

Benchmark.bm(30) do |x|
   x.report("Array#[0]"){
      a = [1,2,3]
      MAX.times{ a[0] }
   }

   x.report("Array#[1]"){
      a = [1,2,3]
      MAX.times{ a[1] }
   }

   x.report("Array#[-1]"){
      a = [1,2,3]
      MAX.times{ a[-1] }
   }

   x.report("Array#[1,2]"){
      a = [1,2,3]
      MAX.times{ a[1,2] }
   }

   x.report("Array#[1..2]"){
      a = [1,2,3]
      MAX.times{ a[1..2] }
   }

   x.report("Array#[-2..-1]"){
      a = [1,2,3]
      MAX.times{ a[-2..-1] }
   }
end
