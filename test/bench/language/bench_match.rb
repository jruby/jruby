require 'benchmark'

def bench_match(bm)
  bm.report("control: failing .match") do
    x = 'x'
    y = /y/
    a = 0
    while a < 1_000_000
      y.match(x)
      a += 1
    end
  end
  
  bm.report("control: matching .match") do
    x = 'x'
    y = /x/
    a = 0
    while a < 1_000_000
      y.match(x)
      a += 1
    end
  end
  
  bm.report("failing regexp conditional (match)") do
    $_ = 'x'
    a = 0
    while a < 1_000_000
      unless /y/
        a += 1
      end
    end
  end
  
  bm.report("matching regexp conditional (match)") do
    $_ = 'x'
    a = 0
    while a < 1_000_000
      if /x/ 
        a += 1
      end
    end
  end
  
  bm.report("failing =~ (match2)") do
    x = 'x'
    y = /y/
    a = 0
    while a < 1_000_000
      y =~ x
      a += 1
    end
  end
  
  bm.report("matching =~ (match2)") do
    x = 'x'
    y = /x/
    a = 0
    while a < 1_000_000
      y =~ x 
      a += 1
    end
  end
  
  bm.report("failing =~ (match3)") do
    x = 'x'
    y = /y/
    a = 0
    while a < 1_000_000
      x =~ y 
      a += 1
    end
  end
  
  bm.report("matching =~ (match3)") do
    x = 'x'
    y = /x/
    a = 0
    while a < 1_000_000
      x =~ y 
      a += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_match(bm)} }
end