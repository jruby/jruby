require 'benchmark'

def bench_local_vars(bm)
  bm.report 'With nested closure, 1M * 100 local var accesses' do
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

  bm.report 'With nested closure, 1M * 100 local var assignments and retrievals' do
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

  bm.report 'From nested closure, 1M * 100 local var accesses' do
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

  bm.report 'From nested closure, 1M * 100 local var assignments and retrievals' do
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

  bm.report 'With nested closure and 3 vars, 1M * 100 local var accesses' do
    a1 = nil
    a2 = nil
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

  bm.report 'With nested closure and 3 vars, 1M * 100 local var assignments and retrievals' do
    a1 = nil
    a2 = nil
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

  bm.report 'From nested closure and 3 vars, 1M * 100 local var accesses' do
    a1 = nil
    a2 = nil
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

  bm.report 'From nested closure and 3 vars, 1M * 100 local var assignments and retrievals' do
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
