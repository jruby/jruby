require 'benchmark'

puts "Control: 100k loops accessing a local variable 100 times"
10.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 100000
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    i += 1;
  end
}
}

puts "Test STI: 100k loops accessing a fixnum var and calling to_i 100 times"
10.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 100000
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    i += 1;
  end
}
}

puts "Test non-STI: 100k loops accessing an Array var and calling to_a 100 times"
10.times {
puts Benchmark.measure {
  a = []; 
  i = 0;
  while i < 100000
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a; a.to_a;
    i += 1;
  end
}
}

def foo
  self
end

puts "Test interpreted: 100k loops calling self's foo 100 times"
10.times {
puts Benchmark.measure {
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
}
}
