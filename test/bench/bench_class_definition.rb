require 'benchmark'

Benchmark.bm(20) do |bench|
  bench.report("10000 def/undef method") {
    class SomeClass
      10000.times {
        eval "def mymethod; end; undef mymethod;"
      }
    end
  }
end
