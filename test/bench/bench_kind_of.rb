require 'benchmark'

class A; end
class B < A; end
class C < B; end
class D < C; end
class E < D; end

class MyString < String; end

e = E.new
m = MyString.new

puts "1m kind_of? on a five-class hierarchy"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < 1000000
      e.kind_of? A
      i += 1
    end
  }
}

puts "1m kind_of on a subclass of String"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < 1000000
      m.kind_of? String
      i += 1
    end
  }
}
