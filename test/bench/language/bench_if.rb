require 'benchmark'

def bench_if(bm)
  def foo1
    a = 0
    b = false
    while a < 10_000
      a += 1
      # include as many local vars and literals as 'if' version will touch
      b; true
    end
  end
  bm.report("control: 1000x normal 10k loop") do
    1000.times { foo1 }
  end
  
  def foo2
    a = 0
    b = false
    while a < 10_000
      a += 1
      if b
        b = false
      else
        b = true
      end
    end
  end
  
  bm.report("1000x 10k loop with if block") do
    1000.times { foo2 }
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_if(bm)} }
end
