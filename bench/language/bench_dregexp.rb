require 'benchmark'

def bench_dregexp(bm)
  bm.report("1m x10 /abcd#{'efgh'}ijkl/") do
    1_000_000.times do
      /abcd#{'efgh'}ijkl/; /abcd#{'efgh'}ijkl/
      /abcd#{'efgh'}ijkl/; /abcd#{'efgh'}ijkl/
      /abcd#{'efgh'}ijkl/; /abcd#{'efgh'}ijkl/
      /abcd#{'efgh'}ijkl/; /abcd#{'efgh'}ijkl/
      /abcd#{'efgh'}ijkl/; /abcd#{'efgh'}ijkl/
    end
  end
  bm.report("1m x10 /abcd#{'efgh'}ijkl/u") do
    1_000_000.times do
      /abcd#{'efgh'}ijkl/u; /abcd#{'efgh'}ijkl/u
      /abcd#{'efgh'}ijkl/u; /abcd#{'efgh'}ijkl/u
      /abcd#{'efgh'}ijkl/u; /abcd#{'efgh'}ijkl/u
      /abcd#{'efgh'}ijkl/u; /abcd#{'efgh'}ijkl/u
      /abcd#{'efgh'}ijkl/u; /abcd#{'efgh'}ijkl/u
    end
  end
  bm.report("1m x10 /abcd#{'efgh'}ijkl/o") do
    1_000_000.times do
      /abcd#{'efgh'}ijkl/o; /abcd#{'efgh'}ijkl/o
      /abcd#{'efgh'}ijkl/o; /abcd#{'efgh'}ijkl/o
      /abcd#{'efgh'}ijkl/o; /abcd#{'efgh'}ijkl/o
      /abcd#{'efgh'}ijkl/o; /abcd#{'efgh'}ijkl/o
      /abcd#{'efgh'}ijkl/o; /abcd#{'efgh'}ijkl/o
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_dregexp(bm)} }
end
