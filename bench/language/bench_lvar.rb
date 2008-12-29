require 'benchmark'

def one
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times { }
  while a < 1_000_000
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a += 1
  end
end

def two
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times {}
  while a < 1_000_000
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a += 1
  end
end

def three
  a = 1
  1.times {
    while a < 1_000_000
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a += 1
    end
  }
end

def four
  a = 1
  1.times {
    while a < 1_000_000
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a += 1
    end
  }
end

# We have special heap-based assignment paths for up to 4 vars, so the next
# several benchmarks use five variables

def five
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times { }
  while a < 1_000_000
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a += 1
  end
end

def six
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times { }
  while a < 1_000_000
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a += 1
  end
end

def seven
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  1.times {
    while a < 1_000_000
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a; a; a; a; a; a; a; a; a; a
      a += 1
    end
  }
end

def eight
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  1.times {
    while a < 1_000_000
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
      a += 1
    end
  }
end

def nine
  a = 1
  while a < 1_000_000
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a; a; a; a; a; a; a; a; a; a
    a += 1
  end
end

def ten
  a = 1
  while a < 1_000_000
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a; a=a
    a += 1
  end
end

def bench_lvar(bm)
  bm.report 'near closure, 1m x100 gets' do one end

  bm.report 'near closure, 1m x100 gets and sets' do two end

  bm.report 'in closure, 1m x100 gets' do three end

  bm.report 'in closure, 1m x100 gets and sets' do four end

  bm.report 'near closure, 5 vars, 1m * x100 gets' do five end

  bm.report 'near closure, 5 vars, 1m * x100 gets and sets' do six end

  bm.report 'in closure, 5 vars, 1m x100 gets' do seven end

  bm.report 'in closure, 5 vars, 1M x100 gets and sets' do eight end
  
  bm.report 'normal heapless, 1m x 100 gets' do nine end
  
  bm.report 'normal heapless, 1m x 100 gets and sets' do ten end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_lvar(bm)} }
end
