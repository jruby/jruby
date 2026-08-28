require 'benchmark'

def bench_array(bm)
  arr = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]

  bm.report("2m array.map, 20 Fixnum array") do
    i = 0
    while i<2000000
      arr.map {|e| e}
      i+=1
    end
  end
  bm.report("2m array.collect, 20 Fixnum array") do
    i = 0
    while i<2000000
      arr.collect {|e| e}
      i+=1
    end
  end
end

if $0 == __FILE__
  if ARGV[0]
    ARGV[0].to_i.times {
      Benchmark.bm(40) {|bm| bench_array(bm)}
    }
  else
    Benchmark.bmbm {|bm| bench_array(bm)}
  end
end
