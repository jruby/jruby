require 'benchmark'

def bench_local_vars(bm)
  bm.report 'near closure, 100k x100 gets' do
    a = 1
    # contained closure forces heap-based vars in compatibility mode
    1.times { }
    while a < 100_000
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

  bm.report 'near closure, 100k x100 gets and sets' do
    a = 1
    # contained closure forces heap-based vars in compatibility mode
    1.times {}
    while a < 100_000
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

  bm.report 'in closure, 100k x100 gets' do
    a = 1
    1.times {
      while a < 100_000
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

  bm.report 'in closure, 100k x100 gets and sets' do
    a = 1
    1.times {
      while a < 100_000
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

  bm.report 'near closure, 3 vars, 100k * x100 gets' do
    a1 = nil
    a2 = nil
    a = 1
    # contained closure forces heap-based vars in compatibility mode
    1.times { }
    while a < 100_000
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

  bm.report 'near closure, 3 vars, 100k * x100 gets and sets' do
    a1 = nil
    a2 = nil
    a = 1
    # contained closure forces heap-based vars in compatibility mode
    1.times { }
    while a < 100_000
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

  bm.report 'in closure, 3 vars, 100k x100 gets' do
    a1 = nil
    a2 = nil
    a = 1
    1.times {
      while a < 100_000
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

  bm.report 'in closure, 3 vars, 1M x100 gets and sets' do
    a1 = nil
    a2 = nil
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
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_local_vars(bm)} }
end
