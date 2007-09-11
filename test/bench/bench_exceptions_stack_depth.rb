require 'benchmark'

def foo(count)
  foo(count - 1) if (count > 0)

  i = 0;
  while i < 10000
    i += 1
    begin
      raise ArgumentError("Hoohaw")
    rescue
    end
  end
end

puts Benchmark.measure { foo(1) }
puts Benchmark.measure { foo(20) }
puts Benchmark.measure { foo(40) }
puts Benchmark.measure { foo(100) }

