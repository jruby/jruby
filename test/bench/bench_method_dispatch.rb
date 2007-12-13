require 'benchmark'

puts "Control: 1m loops accessing a local variable 100 times"
5.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 1000000
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

puts "Test STI: 1m loops accessing a fixnum var and calling to_i 100 times"
5.times {
puts Benchmark.measure {
  a = 5; 
  i = 0;
  while i < 1000000
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

def foo
  self
end

puts "Test ruby method: 1m loops calling self's foo 100 times"
5.times {
puts Benchmark.measure {
  a = []; 
  i = 0;
  while i < 1000000
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

class Object
  define_method(:bar) { }
end

puts "Test define_method method, 100k loops calling bar 100 times"
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
