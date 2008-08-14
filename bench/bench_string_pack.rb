require 'benchmark'

def bench_pack(bm)
  large_str_ary = ["X" * 2_000_000]
  bm.report("pack('m'), large string") do
    large_str_ary.pack('m')
  end
end

if $0 == __FILE__
  Benchmark.bmbm {|bm| bench_pack(bm)}
end
