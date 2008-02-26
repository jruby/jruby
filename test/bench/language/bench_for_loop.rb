require 'benchmark'

def bench_for_loop(bm)
  bm.report "100k x5 for loops of 10 elements" do
    a = 1
    while a < 100_000
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
      a += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_for_loop(bm)} }
end
