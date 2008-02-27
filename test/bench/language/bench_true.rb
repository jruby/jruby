require 'benchmark'

def bench_true(bm)
  bm.report("10m x10 true") do
    10_000_000.times do
      true; true; true; true; true
      true; true; true; true; true
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_true(bm)} }
end
