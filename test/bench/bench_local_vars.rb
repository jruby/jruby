require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

puts 'With nested closure, 1M * 100 local var accesses'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'With nested closure, 1M * 100 local var assignments and retrievals'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'From nested closure, 1M * 100 local var accesses'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'From nested closure, 1M * 100 local var assignments and retrievals'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'With nested closure and 3 vars, 1M * 100 local var accesses'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'With nested closure and 3 vars, 1M * 100 local var assignments and retrievals'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'From nested closure and 3 vars, 1M * 100 local var accesses'
TIMES.times {
  puts Benchmark.measure {
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
  }
}

puts 'From nested closure and 3 vars, 1M * 100 local var assignments and retrievals'
TIMES.times {
  puts Benchmark.measure {
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
  }
}
