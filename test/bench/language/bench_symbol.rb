require 'benchmark'

puts "Control: 100k loops accessing a local symbol 100 times"
10.times {
puts Benchmark.measure {
  i = 0;
  while i < 100000
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
    i += 1;
  end
}
}

