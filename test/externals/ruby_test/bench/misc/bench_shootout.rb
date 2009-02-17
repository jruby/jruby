$LOAD_PATH.unshift(Dir.pwd)

require "benchmark"
require "shootout"

MAX = 20000

Benchmark.bm(20) do |b|
   b.report("ackermann"){
      MAX.times{ Shootout.ack(2,11) }
   }

   b.report("sieve"){
      MAX.times{ Shootout.sieve(11) }
   }

   b.report("harmonic"){
      MAX.times{ Shootout.harmonic(11) }
   }
   
   b.report("fibonacci"){
      MAX.times{ Shootout.fib(11) }
   }
   
   b.report("fannkuch"){
      MAX.times{ Shootout.fannkuch(3) }
   }
end
