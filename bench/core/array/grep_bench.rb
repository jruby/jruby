require 'benchmark'

TIMES = 2_000_000

def bench_array(bm)
  arr1 = ['a', 'b', 'c', 'd', 'e']

  bm.report("grep(/./) { |e| e } (5 element string array)") do
    TIMES.times { arr1.grep(/./) { |e| e } }
  end
  bm.report("grep(/0/) (5 element string array)") do
    TIMES.times { arr1.grep(/0/) }
  end

  arr2 = (0...30).to_a.map { |e| e.to_s }

  bm.report("grep(/./) { |e| e } (30 element string array)") do
    TIMES.times { arr2.grep(/./) { |e| e } }
  end
  bm.report("grep(/0/) (30 element string array)") do
    TIMES.times { arr2.grep(/0/) }
  end
end

if $0 == __FILE__
  if ARGV[0]
    ARGV[0].to_i.times {
      Benchmark.bm(30) { |bm| bench_array(bm) }
    }
  else
    Benchmark.bmbm { |bm| bench_array(bm) }
  end
end
