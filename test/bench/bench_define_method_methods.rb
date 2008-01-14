require 'benchmark'

class Object
  define_method(:foo) { }
  define_method(:bar) {a = 1}
  b = 1
  define_method(:baz) {b = 2}
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

puts "define_method(:bar) {a = 1}, 100k * 100 invocations"
5.times {
  puts Benchmark.measure {
    a = 0
    while a < 100000
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      a += 1
    end
  }
}

puts "b = 1; define_method(:baz) {b = 2}, 100k * 100 invocations"
5.times {
  puts Benchmark.measure {
    a = 0
    while a < 100000
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      a += 1
    end
  }
}
