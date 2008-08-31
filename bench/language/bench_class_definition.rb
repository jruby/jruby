require 'benchmark'

def bench_class_definition(bm)
  bm.report("10000 def/undef method") {
    class << self
      10000.times {
        def my_bogus_method; end; undef my_bogus_method
      }
    end
  }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_class_definition(bm)} }
end