require 'benchmark'

def bench_hash_each(bm)
  hash = { 1 => '1', 2 => '2', 3 => '3', 4 => '4', 5 => '5', 6 => '6',
    7 => '7', 8 => '8', 9 => '9', 10 => '10', 11 => '11', 12 => '12'}

  bm.report("1m hash.each/each_pair, 12 element hash") do
    i = 0
    while i < 1_000_000
      hash.each { |k, v| }
      i += 1
    end
  end
end

if $0 == __FILE__
  if ARGV[0]
    ARGV[0].to_i.times {
      Benchmark.bm(40) {|bm| bench_hash_each(bm)}
    }
  else
    Benchmark.bmbm {|bm| bench_hash_each(bm)}
  end
end
