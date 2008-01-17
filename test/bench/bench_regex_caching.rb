require 'benchmark'

5.times {
  puts Benchmark.measure {
    a = "foo"
    i=0
    while i<200000
        /foo#{a}bar/
        i+=1
    end
  }
}
