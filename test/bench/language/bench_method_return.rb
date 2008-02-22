require 'benchmark'

def foo1; 1; end
def foo2; return 1; end

puts "Test implicit return: 100k loops calling a method 100 times with implicit return"
10.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 100000
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    i += 1;
  end
}
}

puts "Test explicit return: 100 k loops calling a method 100 times with explicit return"
10.times {
puts Benchmark.measure {
  a = []; 
  i = 0;
  while i < 100000
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    i += 1;
  end
}
}
