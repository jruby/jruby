require 'benchmark'

def bench_globals(bm)
  bm.report("1m x10 $foo = self") do
    1_000_000.times do
      $foo = self; $foo = self
      $foo = self; $foo = self
      $foo = self; $foo = self
      $foo = self; $foo = self
      $foo = self; $foo = self
    end
  end
  bm.report("1m x10 $foo") do
    1_000_000.times do
      $foo; $foo; $foo; $foo; $foo; $foo; $foo; $foo; $foo; $foo
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_globals(bm)} }
end
