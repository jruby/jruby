require 'benchmark'

10.times {
  puts Benchmark.measure {
    a = 1
    # contained closure forces heap-based vars in compatibility mode
    1.times { }
    while a < 10_000_000
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
