require 'benchmark'

def foo
  self
end

def invoking
  i = 0;
  while i < 1000000
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    i += 1;
  end
end

puts "Test ruby method: 1000k loops calling self's foo 10 times"
(ARGV[0] || 10).to_i.times {
  puts Benchmark.measure {
    invoking
  }
}
