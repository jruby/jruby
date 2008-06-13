require 'benchmark'

def bench_block_arg(bm)
  def foo1
    1
  end
  
  bm.report("10m calls with no block, no arg") do
    10_000_000.times { foo1 }
  end
  bm.report("10m calls with block, no arg") do
    10_000_000.times { foo1 {} }
  end
  
  def foo2(&b)
  end
    
  bm.report("10m falls with no block,  arg") do
    10_000_000.times { foo2 }
  end
  bm.report("10m falls with block arg") do
    10_000_000.times { foo2 {} }
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_block_arg(bm)} }
end
