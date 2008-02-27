require 'benchmark'

def bench_fixnum(bm)
  bm.report("10m x10 fixnums > 0 and < 128") do
    10_000_000.times { 1; 1; 1; 1; 1; 1; 1; 1; 1; 1 }
  end
  bm.report("10m x10 fixnums > 128") do
    10_000_000.times { 150; 150; 150; 150; 150; 150; 150; 150; 150; 150 }
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_fixnum(bm)} }
end
