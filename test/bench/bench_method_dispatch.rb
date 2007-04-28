require 'benchmark'

def foo
a = 1
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
end

def foo2
a = 5
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
end

puts "Control: 100k loops accessing a local variable 100 times"
10.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 100000
foo
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
foo2
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
