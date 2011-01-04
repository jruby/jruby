require 'benchmark'

def foo(a)
  self
end

def invoking
  i = 0;
  while i < 100000
    foo(:a => :b, :c => :d)
    foo(:a => :b, :c => :d)
    foo(:a => :b, :c => :d)
    foo(:a => :b, :c => :d)
    foo(:a => :b, :c => :d)
    i += 1;
  end
end

puts "Test Hash-args method: 100k loops calling foo(:a => :b, :c => :d) 5 times"
(ARGV[0] || 10).to_i.times {
  puts Benchmark.measure {
    invoking
  }
}
