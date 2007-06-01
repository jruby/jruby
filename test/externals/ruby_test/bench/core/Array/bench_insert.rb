#########################################################
# bench_insert.rb
#
# Benchmark suite for the Array#insert instance method.
#########################################################
require "benchmark"

MAX = 30000

Benchmark.bm(30) do |x|
   x.report("Array#insert(2)"){
      array = [1,2,3,4]
      MAX.times{ array.insert(2, "a", "b") }
   }

   # Test the -1 index specifically, since all it does is append
   x.report("Array#insert(-1)"){
      array = [1,2,3,4]
      MAX.times{ array.insert(-1, "a", "b") }
   }

   x.report("Array#insert(-2)"){
      array = [1,2,3,4]
      MAX.times{ array.insert(-2, "a", "b") }
   }
end
