require 'benchmark'

def bench_dstr(bm)
  bm.report("1m x100 \"abcd\#{'efgh'}ijkl") do
    1_000_000.times do
      "abcd#{'efgh'}ijkl"; "abcd#{'efgh'}ijkl"
      "abcd#{'efgh'}ijkl"; "abcd#{'efgh'}ijkl"
      "abcd#{'efgh'}ijkl"; "abcd#{'efgh'}ijkl"
      "abcd#{'efgh'}ijkl"; "abcd#{'efgh'}ijkl"
      "abcd#{'efgh'}ijkl"; "abcd#{'efgh'}ijkl"
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_dstr(bm)} }
end
