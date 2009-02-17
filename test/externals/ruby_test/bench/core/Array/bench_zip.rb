#########################################################
# bench_zip.rb
#
# Benchmark suite for the Array#zip instance method.
#########################################################
require "benchmark"

MAX = 900000

Benchmark.bm(40) do |x|
   x.report("Array#zip([])"){
      array = [1,2,3]
      MAX.times{ array.zip() }
   }

   x.report("Array#zip([1,2,3])"){
      array = [1,2,3]
      MAX.times{ array.zip([1,2,3]) }
   }

   x.report("Array#zip([1,2,3], ['a','b','c'])"){
      array = [1,2,3]
      MAX.times{ array.zip([1,2,3], ['a','b','c']) }
   }
end
