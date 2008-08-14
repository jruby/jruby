require 'benchmark'

def bench_dvar(bm)
  bm.report 'near closure, 1000k x100 gets' do
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

  bm.report 'near closure, 1000k x100 gets and sets' do
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

  bm.report 'in closure, 1000k x100 gets' do
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

  bm.report 'in closure, 1000k x100 gets and sets' do
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

  bm.report 'near closure, 5 vars, 1000k * x100 gets' do
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

  bm.report 'near closure, 5 vars, 1000k * x100 gets and sets' do
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

  bm.report 'in closure, 5 vars, 1000k x100 gets' do
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

  bm.report 'in closure, 5 vars, 1M x100 gets and sets' do
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
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_dvar(bm)} }
end
