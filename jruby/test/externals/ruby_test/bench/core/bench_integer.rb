#######################################################################
# bench_integer.rb
#
# Benchmark suite for the Integer methods.  Deprecated methods and
# aliases are not benchmarked. To avoid using Integer methods within
# the test suite, we use for loops instead of Integer#times.
#######################################################################
require "benchmark"

MAX = 200000

Benchmark.bm(35) do |x|
   x.report("Integer#chr"){
      for i in 1..MAX
         65.chr
      end
   }

   x.report("Integer#downto"){
      for i in 1..MAX
         10.downto(1){|n| }
      end
   }

   x.report("Integer#floor"){
      for i in 1..MAX
         (-1).floor
      end
   }

   x.report("Integer#integer?"){
      for i in 1..MAX
         100.integer?
      end
   }

   x.report("Integer#next"){
      for i in 1..MAX
         100.next
      end
   }

   x.report("Integer#times"){
      for i in 1..MAX
         10.times{ |n| }
      end
   }

   x.report("Integer#to_i"){
      for i in 1..MAX
         10.to_i
      end
   }

   x.report("Integer#upto"){
      for i in 1..MAX
         0.upto(10){ |n| }
      end
   }
end
