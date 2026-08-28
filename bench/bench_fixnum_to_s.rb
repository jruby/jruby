require 'benchmark/ips'

def foo(a)
  a.to_s
end

Benchmark.ips do |bm|
  bm.report("byte-ranged") do
    n = -256
    while n < 256
      foo(n)
      n+=1
    end
  end

  bm.report("larger") do
    n = 256
    while n < 768
      foo(n)
      n+=1
    end
  end
end