require 'benchmark'

def bench_enumerable(bm)
  arr = [false, false, false, false, false, false, false, false, false, false]

  bm.report("1m array.any?, 10-false array") do
    i = 0
    while i<1000000
      arr.any?
      i+=1
    end
  end
end

if $0 == __FILE__
  if ARGV[0]
    ARGV[0].to_i.times {
      Benchmark.bm(40) {|bm| bench_enumerable(bm)}
    }
  else
    Benchmark.bmbm {|bm| bench_enumerable(bm)}
  end
end
