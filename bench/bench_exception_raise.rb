require 'benchmark/ips'

def foo(depth, count)
  if depth > 0
    foo(depth - 1, count)
  else
    i = 0
    while i < count
      i += 1
      begin
        raise ArgumentError
      rescue
      end
    end
  end
end

Benchmark.ips do |bm|
  bm.report("exception raising at depth 1") do |n|
    foo(1, n)
  end
  bm.report("exception raising at depth 10") do |n|
    foo(10, n)
  end
  bm.report("exception raising at depth 50") do |n|
    foo(50, n)
  end
  bm.report("exception raising backtrace given") do |n|
    i = 0
    while i < n
      i += 1
      begin
        raise ArgumentError, 'foo', nil
      rescue
      end
    end
  end
end

