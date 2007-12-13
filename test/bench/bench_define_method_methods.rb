require 'benchmark'

class Object
  define_method(:foo) { }
end

puts "define_method(:foo) {}, 100k * 100 invocations"
5.times {
  puts Benchmark.measure {
    a = 0
    while a < 100000
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      a += 1
    end
  }
}
