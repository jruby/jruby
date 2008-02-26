require 'benchmark'

def bench_def_method
  x = 0
  while x < 1_000_000
    def a; 1 + 1; end
    x += 1
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_def_method(bm)} }
end
