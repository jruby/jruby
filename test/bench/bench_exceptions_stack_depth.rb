require 'benchmark'

def foo(count)
  if (count > 0)
    foo(count - 1)
  else
    puts Benchmark.measure {
      i = 0;
      while i < 500000
        i += 1
        begin
          raise ArgumentError("Hoohaw")
        rescue
        end
      end
    }
  end
end

foo(1)
foo(40)
foo(100)
foo(400)

