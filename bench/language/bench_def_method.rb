require 'benchmark'

def bench_def_method(bm)
  bm.report("100k def a; 1 + 1; end") do
    Class.new do
      x = 0
      while x < 100_000
        def a; 1 + 1; end
        x += 1
      end
    end
  end
  bm.report("100k toplevel def a; 1 + 1; end") do
    x = 0
    while x < 100_000
      def a; 1 + 1; end
      x += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_def_method(bm)} }
end
