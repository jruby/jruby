require 'benchmark'

def bench_block_arg(bm)
  def foo1
    1
  end
  
  bm.report("control: 10m.times") do
    10_000_000.times { }
  end
  bm.report("10m calls with no block, no arg") do
    10_000_000.times { foo1 }
  end
  bm.report("10m calls with block, no arg") do
    10_000_000.times { foo1 {} }
  end
  
  def foo2(&b)
  end
    
  bm.report("10m calls with no block,  arg") do
    10_000_000.times { foo2 }
  end
  bm.report("10m calls with block arg") do
    10_000_000.times { foo2 {} }
  end

  a = proc {}

  bm.report("10m calls with block pass arg") do
    10_000_000.times { foo2(&a) }
  end

end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_block_arg(bm)} }
end
