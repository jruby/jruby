require 'benchmark'

def foo
  return 1;
end

def bar
  1;
end

puts "Explicit return"
5.times do
  puts Benchmark.measure {
    i = 0
    while i < 100000
      i += 1
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
    end
  }
end

puts "Implicit return"
5.times do
  puts Benchmark.measure {
    i = 0
    while i < 100000
      i += 1
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
    end
  }
end
