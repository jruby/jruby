require 'benchmark'

class A; end
class B < A; end
class C < B; end
class D < C; end
class E < D; end

e = E.new

10.times {
  puts Benchmark.measure {
    i = 0
    while i < 1000000
      e.kind_of? A
      i += 1
    end
  }
}
