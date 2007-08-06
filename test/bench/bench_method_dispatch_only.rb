require 'benchmark'

def foo
  self
end

def invoking
  a = []; 
  i = 0;
  while i < 100000
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    i += 1;
  end
end

puts "Test interpreted: 100k loops calling self's foo 100 times"
10.times {
  puts Benchmark.measure {
    invoking
  }
}
