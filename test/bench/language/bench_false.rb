require 'benchmark'

def bench_false(bm)
  bm.report("10m x10 false") do
    10_000_000.times do
      false; false; false; false; false
      false; false; false; false; false
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_false(bm)} }
end
