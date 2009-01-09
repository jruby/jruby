require 'benchmark'

def zero
  a = 1
  while a < 1_000_000
    a += 1
  end
end

def one
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times { }
  while a < 1_000_000
    a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a
    a += 1
  end
end

def two
  a = 1
  1.times {
    while a < 1_000_000
      a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a
      a += 1
    end
  }
end

# We have special heap-based assignment paths for up to 4 vars, so the next
# several benchmarks use five variables

def three
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  # contained closure forces heap-based vars in compatibility mode
  1.times { }
  while a < 1_000_000
    a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a
    a += 1
  end
end

def four
  a1 = nil
  a2 = nil
  a3 = nil
  a4 = nil
  a = 1
  1.times {
    while a < 1_000_000
      a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a
      a += 1
    end
  }
end

def five
  a = 1
  while a < 1_000_000
    a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a; a,a=a,a
    a += 1
  end
end

def six
  a = 1
  while a < 1_000_000
    a,b,c,d,e,f,g,h,i,j=a,b,c,d,e,f,g,h,i,j
    a += 1
  end
end

def bench_masgn(bm)
  bm.report 'control, 1m while loop' do one end

  bm.report 'near closure, 1m x10 a,a=a,a' do one end

  bm.report 'in closure, 1m x10 a,a=a,a' do two end

  bm.report 'near closure, 5 vars, 1m * x10 a,a=a,a' do three end

  bm.report 'in closure, 5 vars, 1m x10 a,a=a,a' do four end

  bm.report 'normal heapless, 1m x 100 a,a=a,a' do five end

  bm.report 'normal heapless, 1m x 100 10-var masgn' do five end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_masgn(bm)} }
end
