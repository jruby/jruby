require 'benchmark'

def bench_literal_string(bm)
  bm.report("1m x100 \"abcdijkl\"") do
    1_000_000.times do
      a = "abcdijkl"; a = "abcdijkl"
      a = "abcdijkl"; a = "abcdijkl"
      a = "abcdijkl"; a = "abcdijkl"
      a = "abcdijkl"; a = "abcdijkl"
      a = "abcdijkl"; a = "abcdijkl"
      a
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_literal_string(bm)} }
end
