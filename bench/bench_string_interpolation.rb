require 'benchmark'

loop {
  puts Benchmark.measure {
    n = 0
    while n < 100000
      n+=1
      a = nil
      i = 0
      while i < 1000
        i+=1
        a = "foo#{n}bar"
      end
    end
  }
}
