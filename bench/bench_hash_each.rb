require 'benchmark'

def bench_hash_each(bm)
  hash = { 1 => '1', 2 => '2', 3 => '3', 4 => '4', 5 => '5', 6 => '6',
    7 => '7', 8 => '8', 9 => '9', 10 => '10', 11 => '11', 12 => '12',
    13 => '13', 14 => '8', 15 => '9', 16 => '10', 17 => '11', 18 => '12',
    23 => '13', 24 => '8', 25 => '9', 26 => '10', 27 => '11', 28 => '12',
    33 => '13', 34 => '8', 35 => '9', 36 => '10', 37 => '11', 38 => '12',
    43 => '13', 44 => '8', 45 => '9', 46 => '10', 47 => '11', 48 => '12',
    53 => '13', 54 => '8', 55 => '9', 56 => '10', 57 => '11', 58 => '12',
    63 => '13', 64 => '8', 65 => '9', 66 => '10', 67 => '11', 68 => '12'
  }

  bm.report("1m hash.each w/ 2-arg block, 12 element hash") do
    i = 0
    while i < 1_000_000
      hash.each { |k, v| }
      i += 1
    end
  end

  bm.report("1m hash.each w/ 1-arg block, 12 element hash") do
    i = 0
    while i < 1_000_000
      hash.each { |k| }
      i += 1
    end
  end

  bm.report("1m hash.each_value w/ 1-arg block, 12 element hash") do
    i = 0
    while i < 1_000_000
      hash.each_value { |k| }
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
