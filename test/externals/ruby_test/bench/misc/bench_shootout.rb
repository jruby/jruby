$LOAD_PATH.unshift(Dir.pwd)

require "benchmark"
require "shootout"

MAX = 20000

Benchmark.bm(20) do |b|
   b.report("ackermann"){
      MAX.times{ Shootout.ack(1,9) }
   }

   b.report("sieve"){
      MAX.times{ Shootout.sieve(9) }
   }

   b.report("harmonic"){
      MAX.times{ Shootout.harmonic(10) }
   }
end
