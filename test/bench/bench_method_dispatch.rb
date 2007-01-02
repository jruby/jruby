require 'benchmark'

puts "Control: 100k loops accessing a local variable 100 times"
10.times {
puts Benchmark.measure {
  t = Time.now; a = 5; 
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
  puts Time.now - t;
}
}

puts "Test: 100k loops accessing a fixnum var and calling to_i 100 times"
100.times {
puts Benchmark.measure {
  t = Time.now; a = 5; 
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
  puts Time.now - t;
}
}
